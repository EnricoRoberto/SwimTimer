package com.swimtimer.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SessionStorage {
    private static final String PREFS = "swim_sessions";
    private static final String KEY_IDS = "session_ids";

    public static void saveSession(Context ctx, SessionData s) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", s.getId());
            obj.put("name", s.getName());
            obj.put("date", s.getDate());
            obj.put("totalTime", s.getTotalTime());
            obj.put("photoPath", s.getPhotoPath() != null ? s.getPhotoPath() : "");
            JSONArray lapsArr = new JSONArray();
            for (Long l : s.getLaps()) lapsArr.put(l);
            obj.put("laps", lapsArr);
            prefs.edit().putString("session_" + s.getId(), obj.toString()).apply();

            // Aggiorna lista ID
            List<String> ids = getIds(prefs);
            if (!ids.contains(s.getId())) {
                ids.add(0, s.getId());
                JSONArray idsArr = new JSONArray();
                for (String id : ids) idsArr.put(id);
                prefs.edit().putString(KEY_IDS, idsArr.toString()).apply();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static List<SessionData> loadAll(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<SessionData> list = new ArrayList<>();
        for (String id : getIds(prefs)) {
            SessionData s = load(prefs, id);
            if (s != null) list.add(s);
        }
        return list;
    }

    public static SessionData loadById(Context ctx, String id) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return load(prefs, id);
    }

    private static SessionData load(SharedPreferences prefs, String id) {
        try {
            String json = prefs.getString("session_" + id, null);
            if (json == null) return null;
            JSONObject obj = new JSONObject(json);
            List<Long> laps = new ArrayList<>();
            JSONArray lapsArr = obj.getJSONArray("laps");
            for (int i = 0; i < lapsArr.length(); i++) laps.add(lapsArr.getLong(i));
            SessionData s = new SessionData(
                    obj.getString("name"), obj.getLong("date"),
                    obj.getLong("totalTime"), laps);
            String photo = obj.optString("photoPath", "");
            if (!photo.isEmpty()) s.setPhotoPath(photo);
            return s;
        } catch (Exception e) { return null; }
    }

    public static void renameSession(Context ctx, String id, String newName) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            String json = prefs.getString("session_" + id, null);
            if (json == null) return;
            JSONObject obj = new JSONObject(json);
            obj.put("name", newName);
            prefs.edit().putString("session_" + id, obj.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void deleteSession(Context ctx, String id) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove("session_" + id).apply();
        List<String> ids = getIds(prefs);
        ids.remove(id);
        JSONArray idsArr = new JSONArray();
        for (String i : ids) idsArr.put(i);
        prefs.edit().putString(KEY_IDS, idsArr.toString()).apply();
    }

    private static List<String> getIds(SharedPreferences prefs) {
        List<String> ids = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_IDS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) ids.add(arr.getString(i));
        } catch (Exception e) { e.printStackTrace(); }
        return ids;
    }
}
