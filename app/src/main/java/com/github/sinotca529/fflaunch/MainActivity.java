package com.github.sinotca529.fflaunch;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class MainActivity extends AppCompatActivity {
    private List<AppInfo> appList;
    private AppListAdapter appAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appList = getInstalledApps();
        appAdapter = new AppListAdapter(new ArrayList<>(appList), this);

        setContentView(R.layout.activity_main);

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
            String name = resolveInfo.loadLabel(pm).toString();
            Drawable icon = resolveInfo.loadIcon(pm);
            String packageName = resolveInfo.activityInfo.packageName;
            appInfoList.add(new AppInfo(name, icon, packageName));
        }

        return appInfoList;
    }
}