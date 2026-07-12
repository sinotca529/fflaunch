package com.github.sinotca529.fflaunch;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AliasManager {

    // 毎回 SharedPreferences から読んで Gson でパースすると検索のたびに重いので、
    // メモリ上にキャッシュする。書き換えは addAlias/removeAlias 経由でのみ行うこと。
    private static HashMap<String, List<String>> cache = null;

    private final SharedPreferences sharedPreferences;

    public AliasManager(Context context) {
        sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
    }

    // 返り値はキャッシュそのものなので、呼び出し側で書き換えないこと
    public HashMap<String, List<String>> loadAliasMap() {
        if (cache == null) {
            String json = sharedPreferences.getString("app_alias", "{}");
            Type type = new TypeToken<HashMap<String, List<String>>>() {}.getType();
            cache = new Gson().fromJson(json, type);
            if (cache == null) cache = new HashMap<>();
        }
        return cache;
    }

    public void saveAliasMap(HashMap<String, List<String>> map) {
        cache = map;
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
