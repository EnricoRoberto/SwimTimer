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

    // Risultato confronto record
    public static final int RECORD_NONE        = 0; // nessun confronto possibile
    public static final int RECORD_ABSOLUTE    = 1; // 🥇 record assoluto
    public static final int RECORD_IMPROVED    = 2; // 📈 miglioramento rispetto all'ultima
    public static final int RECORD_BOTH        = 3; // 🥇 + 📈 entrambi

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

    /**
     * Confronta la vasca più veloce della sessione corrente con la cronologia.
     * Usa solo sessioni della stessa specialità (es. "100m Stile Libero").
     * Ignora la sessione corrente (non ancora salvata).
     *
     * @param ctx         contesto
     * @param specialty   specialità della sessione corrente (es. "100m Stile Libero")
     * @param fastestLap  tempo della vasca più veloce della sessione corrente (ms)
     * @param currentId   id della sessione corrente da escludere (null se non ancora salvata)
     * @return RECORD_NONE / RECORD_ABSOLUTE / RECORD_IMPROVED / RECORD_BOTH
     */
    public static int checkRecord(Context ctx, String specialty,
                                   long fastestLap, String currentId) {
        if (specialty == null || specialty.isEmpty()) return RECORD_NONE;

        List<SessionData> all = loadAll(ctx);

        // Filtra solo sessioni della stessa specialità escludendo quella corrente
        List<SessionData> sameSpecialty = new ArrayList<>();
        for (SessionData s : all) {
            if (currentId != null && s.getId().equals(currentId)) continue;
            if (s.getName() != null && s.getName().contains(specialty)) {
                sameSpecialty.add(s);
            }
        }

        if (sameSpecialty.isEmpty()) return RECORD_NONE;

        // Trova il record assoluto (vasca più veloce di sempre)
        long absoluteBest = Long.MAX_VALUE;
        // Trova il best dell'ultima sessione (la più recente per data)
        long lastSessionBest = Long.MAX_VALUE;
        SessionData lastSession = null;

        for (SessionData s : sameSpecialty) {
            // Best assoluto
            if (s.getLaps() != null) {
                for (Long lap : s.getLaps()) {
                    if (lap > 0 && lap < absoluteBest) absoluteBest = lap;
                }
            }
            // Ultima sessione per data
            if (lastSession == null || s.getDate() > lastSession.getDate()) {
                lastSession = s;
            }
        }

        // Best dell'ultima sessione
        if (lastSession != null && lastSession.getLaps() != null) {
            for (Long lap : lastSession.getLaps()) {
                if (lap > 0 && lap < lastSessionBest) lastSessionBest = lap;
            }
        }

        boolean isAbsolute = absoluteBest != Long.MAX_VALUE && fastestLap < absoluteBest;
        boolean isImproved = lastSessionBest != Long.MAX_VALUE && fastestLap < lastSessionBest;

        if (isAbsolute && isImproved) return RECORD_BOTH;
        if (isAbsolute) return RECORD_ABSOLUTE;
        if (isImproved) return RECORD_IMPROVED;
        return RECORD_NONE;
    }

    /**
     * Estrae la specialità da un nome sessione tipo "Mario Rossi — 100m Stile Libero"
     */
    public static String extractSpecialty(String sessionName) {
        if (sessionName == null) return "";
        int sep = sessionName.indexOf(" — ");
        if (sep >= 0) return sessionName.substring(sep + 3);
        return sessionName; // il nome è già solo la specialità
    }

    /**
     * Restituisce l'id della sessione con il tempo totale migliore
     * per una data specialità. Usato per evidenziare il record nella cronologia.
     */
    public static String getBestSessionIdBySpecialty(Context ctx, String specialty) {
        if (specialty == null || specialty.isEmpty()) return null;
        List<SessionData> all = loadAll(ctx);
        String bestId = null;
        long bestTime = Long.MAX_VALUE;
        for (SessionData s : all) {
            String sp = extractSpecialty(s.getName());
            if (sp.equals(specialty) && s.getTotalTime() > 0
                    && s.getTotalTime() < bestTime) {
                bestTime = s.getTotalTime();
                bestId = s.getId();
            }
        }
        return bestId;
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
