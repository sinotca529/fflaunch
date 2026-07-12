package com.github.sinotca529.fflaunch;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class MainActivity extends AppCompatActivity {
    private List<AppInfo> appList = new ArrayList<>();
    private AppListAdapter appAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appAdapter = new AppListAdapter(new ArrayList<>(), this);

        RecyclerView recyclerView = findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(appAdapter);

        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener((new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                List<AppInfo> hit = searchApps(query);
                appAdapter.updateAppList(hit);
                recyclerView.scrollToPosition(0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        }));

        // アプリ一覧の取得は重いのでバックグラウンドで行い、UI は先に表示する
        executor.execute(() -> {
            final var apps = getInstalledApps();

            mainHandler.post(() -> {
                appList = apps;
                final var query = searchBar.getText().toString();
                appAdapter.updateAppList(query.trim().isEmpty() ? apps : searchApps(query));
                recyclerView.scrollToPosition(0);
            });

            // スクロール時のカクつきを防ぐため、アイコンをキャッシュへ先読みしておく
            for (final var app : apps) {
                app.getAppIcon();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private List<AppInfo> searchApps(String query) {
        query = query.trim();
        if (query.isEmpty()) return appList;
        final var finalQuery = StringUtil.regularize(query);

        final var aliasMap = new AliasManager(this).loadAliasMap();
        for (final var aliases : aliasMap.values()) {
            aliases.replaceAll(StringUtil::regularize);
        }

        record InfoScore(AppInfo info, int score) {}

        return appList
            .stream()
            .map((e) -> {
                final var aliases = aliasMap
                    .getOrDefault(e.getPackageName(), new ArrayList<>());
                assert aliases != null;
                aliases.add(StringUtil.regularize(e.getAppName()));

                final var score = aliases
                    .stream()
                    .map(s -> FuzzySearch.ratio(s, finalQuery))
                    .max(Integer::compareTo)
                    .get();

                return new InfoScore(e, score);
            })
            .filter((infoScore) -> infoScore.score() > 0)
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
