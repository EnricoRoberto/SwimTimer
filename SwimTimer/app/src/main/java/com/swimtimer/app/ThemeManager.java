package com.swimtimer.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_MULTICOLOR = 2;

    private static final String PREFS = "swim_prefs";
    private static final String KEY = "theme";
    private static ThemeManager instance;
    private int currentTheme;

    private ThemeManager(Context ctx) {
        currentTheme = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, THEME_LIGHT);
    }

    public static ThemeManager getInstance(Context ctx) {
        if (instance == null) instance = new ThemeManager(ctx.getApplicationContext());
        return instance;
    }

    public int getCurrentTheme() { return currentTheme; }

    public void setTheme(Context ctx, int theme) {
        currentTheme = theme;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, theme).apply();
    }

    public void applyTheme(AppCompatActivity activity) {
        switch (currentTheme) {
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                activity.setTheme(R.style.Theme_SwimTimer_Dark);
                break;
            case THEME_MULTICOLOR:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                activity.setTheme(R.style.Theme_SwimTimer_Multi);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                activity.setTheme(R.style.Theme_SwimTimer);
                break;
        }
    }
}
