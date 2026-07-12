package com.github.sinotca529.fflaunch;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class MainActivity extends AppCompatActivity {
    // 一致度がこの値未満の候補はノイズなので結果に出さない
    private static final int SCORE_THRESHOLD = 40;

    private List<AppInfo> appList = new ArrayList<>();
    // パッケージ名 → 正規化済み別名のリスト
    private Map<String, List<String>> normalizedAliasMap = new HashMap<>();
    private AppListAdapter appAdapter;
    private EditText searchBar;
    private RecyclerView recyclerView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appAdapter = new AppListAdapter(new ArrayList<>(), this);

        recyclerView = findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(appAdapter);

        searchBar = findViewById(R.id.search_bar);
        searchBar.requestFocus();
        searchBar.addTextChangedListener((new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshList();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        }));

        // Enter (Go) キーで先頭の候補を起動する
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO) return false;
            if (searchBar.getText().toString().trim().isEmpty()) return false;

            final var top = appAdapter.getFirstApp();
            if (top != null) appAdapter.launchApp(top.getPackageName());
            return true;
        });

        reloadNormalizedAliasMap();

        // アプリ一覧の取得は重いのでバックグラウンドで行い、UI は先に表示する
        executor.execute(() -> {
            final var apps = getInstalledApps();
            apps.sort(Comparator.comparing(AppInfo::getNormalizedName));

            mainHandler.post(() -> {
                appList = apps;
                refreshList();
            });

            // スクロール時のカクつきを防ぐため、アイコンをキャッシュへ先読みしておく
            for (final var app : apps) {
                app.getAppIcon();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 次に開いたときすぐ打ち始められるよう、クエリを消しておく
        searchBar.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // 別名の追加・削除後に AliasDialogFragment から呼ばれる
    public void onAliasesChanged() {
        reloadNormalizedAliasMap();
        refreshList();
        // 行に表示している別名も更新する (DiffUtil は同一オブジェクトを再バインドしないため)
        appAdapter.notifyDataSetChanged();
    }

    private void reloadNormalizedAliasMap() {
        final var aliasMap = new AliasManager(this).loadAliasMap();
        final var normalized = new HashMap<String, List<String>>();
        for (final var entry : aliasMap.entrySet()) {
            normalized.put(
                entry.getKey(),
                entry.getValue().stream().map(StringUtil::regularize).collect(Collectors.toList())
            );
        }
        normalizedAliasMap = normalized;
    }

    private void refreshList() {
        appAdapter.updateAppList(searchApps(searchBar.getText().toString()));
        recyclerView.scrollToPosition(0);
    }

    private List<AppInfo> searchApps(String query) {
        query = query.trim();
        if (query.isEmpty()) return appList;
        final var finalQuery = StringUtil.regularize(query);

        record InfoScore(AppInfo info, int score) {}

        return appList
            .stream()
            .map((app) -> {
                // 打ちかけの文字列でも部分一致で拾えるよう weightedRatio を使う
                var score = FuzzySearch.weightedRatio(app.getNormalizedName(), finalQuery);

                final var aliases = normalizedAliasMap
                    .getOrDefault(app.getPackageName(), List.of());
                for (final var alias : aliases) {
                    score = Math.max(score, FuzzySearch.weightedRatio(alias, finalQuery));
                }

                return new InfoScore(app, score);
            })
            .filter((infoScore) -> infoScore.score() >= SCORE_THRESHOLD)
            .sorted((a, b) -> b.score() - a.score())
            .map(InfoScore::info)
            .collect(Collectors.toList());
    }

    private List<AppInfo> getInstalledApps() {
        PackageManager pm = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        List<AppInfo> appInfoList = new ArrayList<>();

        for (ResolveInfo resolveInfo : resolveInfos) {
            appInfoList.add(new AppInfo(resolveInfo, pm));
        }

        return appInfoList;
    }
}
