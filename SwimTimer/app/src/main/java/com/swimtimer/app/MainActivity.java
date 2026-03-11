package com.swimtimer.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.swimtimer.app.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final Handler handler = new Handler();
    private long startTime = 0L, elapsedTime = 0L, lastLapTime = 0L;
    private boolean isRunning = false;
    private final List<Long> laps = new ArrayList<>();
    private Vibrator vibrator;
    private ThemeManager themeManager;
    private LapAdapter lapAdapter;

    private final Runnable timerRunnable = new Runnable() {
        @Override public void run() {
            long total = elapsedTime + (System.currentTimeMillis() - startTime);
            binding.tvTime.setText(formatTime(total));
            binding.tvCurrentLap.setText(getString(R.string.current_lap)
                    + " " + formatTime(total - lastLapTime));
            handler.postDelayed(this, 10);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            android.util.Log.e("SWIMCRASH", "CRASH: " + throwable.toString());
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Errore - copialo!")
                    .setMessage(throwable.toString())
                    .setPositiveButton("OK", null)
                    .show());
        });
        try {
            themeManager = ThemeManager.getInstance(this);
            themeManager.applyTheme(this);
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            setSupportActionBar(binding.toolbar);
            binding.waveView.setTheme(themeManager.getCurrentTheme());
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            lapAdapter = new LapAdapter(laps);
            binding.rvLaps.setLayoutManager(new LinearLayoutManager(this));
            binding.rvLaps.setAdapter(lapAdapter);
            binding.btnStartStop.setOnClickListener(v -> { vibrate(); toggleTimer(); });
            binding.btnLap.setOnClickListener(v -> { if (isRunning) { vibrate(); recordLap(); }});
            binding.btnReset.setOnClickListener(v -> { vibrate(); onResetPressed(); });
            updateUI();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Errore onCreate - copialo!")
                    .setMessage(e.toString())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void toggleTimer() {
        if (isRunning) {
            elapsedTime += System.currentTimeMillis() - startTime;
            isRunning = false;
            handler.removeCallbacks(timerRunnable);
            binding.swimmerView.setRunning(false);
        } else {
            startTime = System.currentTimeMillis();
            isRunning = true;
            handler.post(timerRunnable);
            binding.swimmerView.setRunning(true);
        }
        updateUI();
    }

    private void recordLap() {
        long total = elapsedTime + (System.currentTimeMillis() - startTime);
        laps.add(0, total - lastLapTime);
        lastLapTime = total;
        lapAdapter.notifyItemInserted(0);
        binding.rvLaps.scrollToPosition(0);
    }

    private void onResetPressed() {
        if (isRunning) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_reset)
                    .setMessage(R.string.confirm_reset_msg)
                    .setPositiveButton(R.string.yes, (d, w) -> { vibrate(); stopAndAskSave(); })
                    .setNegativeButton(R.string.no, null).show();
        } else if (elapsedTime > 0) {
            stopAndAskSave();
        }
    }

    private void stopAndAskSave() {
        if (isRunning) {
            elapsedTime += System.currentTimeMillis() - startTime;
            isRunning = false;
            handler.removeCallbacks(timerRunnable);
            binding.swimmerView.setRunning(false);
        }
        showSaveDialog();
    }

    private void showSaveDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);
        com.google.android.material.textfield.TextInputEditText et =
                dv.findViewById(R.id.etSessionName);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.save_session)
                .setView(dv)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = et.getText() != null ?
                            et.getText().toString().trim() : "";
                    if (name.isEmpty()) name = getString(R.string.default_session_name)
                            + " " + java.text.DateFormat.getDateTimeInstance()
                            .format(new java.util.Date());
                    List<Long> savedLaps = new ArrayList<>(laps);
                    Collections.reverse(savedLaps);
                    SessionStorage.saveSession(this,
                            new SessionData(name, System.currentTimeMillis(),
                                    elapsedTime, savedLaps));
                    Toast.makeText(this, R.string.session_saved, Toast.LENGTH_SHORT).show();
                    resetAll();
                })
                .setNegativeButton(R.string.discard, (d, w) -> resetAll())
                .setNeutralButton(R.string.cancel, null).show();
    }

    private void resetAll() {
        elapsedTime = 0; lastLapTime = 0; isRunning = false;
        laps.clear();
        lapAdapter.notifyDataSetChanged();
        binding.tvTime.setText("00:00.00");
        binding.tvCurrentLap.setText(getString(R.string.current_lap) + " 00:00.00");
        binding.swimmerView.setRunning(false);
        updateUI();
    }

    private void updateUI() {
        if (isRunning) {
            binding.btnStartStop.setText(R.string.stop);
            binding.btnStartStop.setIconResource(R.drawable.ic_stop);
            binding.btnLap.setEnabled(true);
            binding.btnReset.setEnabled(false);
        } else {
            binding.btnStartStop.setText(R.string.start);
            binding.btnStartStop.setIconResource(R.drawable.ic_play);
            binding.btnLap.setEnabled(false);
            binding.btnReset.setEnabled(elapsedTime > 0);
        }
        if (themeManager.getCurrentTheme() == ThemeManager.THEME_MULTICOLOR) {
            binding.btnStartStop.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF00C853));
            binding.btnLap.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF2979FF));
            binding.btnReset.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFF1744));
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator())
            vibrator.vibrate(VibrationEffect.createOneShot(50,
                    VibrationEffect.DEFAULT_AMPLITUDE));
    }

    public static String formatTime(long ms) {
        long h = ms / 3600000, m = (ms % 3600000) / 60000,
             s = (ms % 60000) / 1000, c = (ms % 1000) / 10;
        return h > 0 ? String.format("%02d:%02d:%02d.%02d", h, m, s, c)
                     : String.format("%02d:%02d.%02d", m, s, c);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class)); return true;
        } else if (id == R.id.action_theme) {
            showThemeDialog(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showThemeDialog() {
        String[] themes = {
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_multi)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_theme)
                .setSingleChoiceItems(themes, themeManager.getCurrentTheme(), (d, w) -> {
                    themeManager.setTheme(this, w); d.dismiss(); recreate();
                }).show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timerRunnable);
    }
}
