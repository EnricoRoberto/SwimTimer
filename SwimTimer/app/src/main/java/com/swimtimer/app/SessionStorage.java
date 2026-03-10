package com.swimtimer.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SessionStorage {
    private static final String PREFS = "sessions_prefs";
    private static final String KEY = "sessions";
    private static final Gson gson = new Gson();

    public static void saveSession(Context ctx, SessionData s) {
        List<SessionData> list = getSessions(ctx);
        list.add(0, s);
        store(ctx, list);
    }

    public static List<SessionData> getSessions(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (json == null) return new ArrayList<>();
        Type t = new TypeToken<List<SessionData>>(){}.getType();
        List<SessionData> l = gson.fromJson(json, t);
        return l != null ? l : new ArrayList<>();
    }

    public static void deleteSession(Context ctx, String id) {
        List<SessionData> l = getSessions(ctx);
        l.removeIf(s -> s.getId().equals(id));
        store(ctx, l);
    }

    public static void updateSession(Context ctx, SessionData updated) {
        List<SessionData> l = getSessions(ctx);
        for (int i = 0; i < l.size(); i++)
            if (l.get(i).getId().equals(updated.getId())) { l.set(i, updated); break; }
        store(ctx, l);
    }

    private static void store(Context ctx, List<SessionData> l) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, gson.toJson(l)).apply();
    }
}
