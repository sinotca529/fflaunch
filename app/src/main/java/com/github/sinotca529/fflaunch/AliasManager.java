package com.github.sinotca529.fflaunch;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AliasManager {

    private final SharedPreferences sharedPreferences;

    public AliasManager(Context context) {
        sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
    }

    public HashMap<String, List<String>> loadAliasMap() {
        String json = sharedPreferences.getString("app_alias", "{}");
        return (new Gson()).fromJson(json, HashMap.class);
    }

    public void saveAliasMap(HashMap<String, List<String>> map) {
        String json = (new Gson()).toJson(map);
        sharedPreferences.edit().putString("app_alias", json).apply();
    }

    // 別名を追加
    public void addAlias(String packageName, String alias) {
        var aliasMap = loadAliasMap();

        if (!aliasMap.containsKey(packageName)) {
            aliasMap.put(packageName, new ArrayList<>());
        }
        aliasMap.get(packageName).add(alias);

        saveAliasMap(aliasMap);
    }

    // 別名を取得
    public List<String> getAliases(String packageName) {
        return loadAliasMap().getOrDefault(packageName, new ArrayList<>());
    }

    // 別名を削除
    public void removeAlias(String packageName, String alias) {
        final var aliasMap = loadAliasMap();
        if (!aliasMap.containsKey(packageName)) return;

        final var aliases = aliasMap.get(packageName);
        assert aliases != null;
        aliases.remove(alias);
        if (aliases.isEmpty()) aliasMap.remove(packageName);

        saveAliasMap(aliasMap);
    }
}
