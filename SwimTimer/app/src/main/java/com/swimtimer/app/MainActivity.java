package com.swimtimer.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.swimtimer.app.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final Handler handler = new Handler();
    private long startTime = 0L, elapsedTime = 0L, lastLapTime = 0L;
    private boolean isRunning = false;
    private final List<Long> laps = new ArrayList<>();
    private Vibrator vibrator;
    private ThemeManager themeManager;
    private LapAdapter lapAdapter;

    // Foto
    private String currentPhotoPath = null;
    private Uri photoUri = null;
    private ImageView dialogPhotoPreview = null;
    private TextView dialogPhotoLabel = null;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentPhotoPath != null) {
                    // Mostra anteprima nel dialog
                    if (dialogPhotoPreview != null) {
                        dialogPhotoPreview.setVisibility(View.VISIBLE);
                        dialogPhotoPreview.setImageURI(Uri.fromFile(new File(currentPhotoPath)));
                    }
                    if (dialogPhotoLabel != null) {
                        dialogPhotoLabel.setText("✅ Foto scattata!");
                        dialogPhotoLabel.setTextColor(Color.parseColor("#4CAF50"));
                    }
                } else {
                    currentPhotoPath = null;
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this,
                        "Permesso fotocamera negato", Toast.LENGTH_SHORT).show();
            });

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
                    .setTitle("Errore onCreate")
                    .setMessage(e.toString())
                    .setPositiveButton("OK", null).show();
        }
    }

    private void toggleTimer() {
        if (isRunning) {
            elapsedTime += System.currentTimeMillis() - startTime;
            isRunning = false;
            handler.removeCallbacks(timerRunnable);
            binding.swimmerView.setRunning(false);
            binding.waveView.setRunning(false);
        } else {
            startTime = System.currentTimeMillis();
            isRunning = true;
            handler.post(timerRunnable);
            binding.swimmerView.setRunning(true);
            binding.waveView.setRunning(true);
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
            elapsedTime += System.currentTimeMillis() - startTime;
            isRunning = false;
            handler.removeCallbacks(timerRunnable);
            binding.swimmerView.setRunning(false);
            binding.waveView.setRunning(false);
            updateUI();
        }
        if (elapsedTime > 0) {
            currentPhotoPath = null;
            showSaveDialog();
        } else {
            resetAll();
        }
    }

    private void showSaveDialog() {
        try {
            View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);
            com.google.android.material.textfield.TextInputEditText et =
                    dv.findViewById(R.id.etSessionName);
            MaterialButton btnPhoto = dv.findViewById(R.id.btnTakePhoto);
            dialogPhotoPreview = dv.findViewById(R.id.ivPhotoPreview);
            dialogPhotoLabel = dv.findViewById(R.id.tvPhotoLabel);

            btnPhoto.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    launchCamera();
                } else {
                    requestCameraPermission.launch(Manifest.permission.CAMERA);
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Salva Gara")
                    .setView(dv)
                    .setPositiveButton(R.string.save, (d, w) -> {
                        String name = et.getText() != null ?
                                et.getText().toString().trim() : "";
                        if (name.isEmpty()) {
                            name = getString(R.string.default_session_name)
                                    + " " + java.text.DateFormat.getDateTimeInstance()
                                    .format(new java.util.Date());
                        }
                        List<Long> savedLaps = new ArrayList<>(laps);
                        Collections.reverse(savedLaps);
                        SessionData session = new SessionData(name,
                                System.currentTimeMillis(), elapsedTime, savedLaps);
                        if (currentPhotoPath != null) {
                            session.setPhotoPath(currentPhotoPath);
                        }
                        SessionStorage.saveSession(this, session);
                        Toast.makeText(this, R.string.session_saved,
                                Toast.LENGTH_SHORT).show();
                        resetAll();
                    })
                    .setNegativeButton(R.string.discard, (d, w) -> {
                        currentPhotoPath = null;
                        resetAll();
                    })
                    .setNeutralButton(R.string.cancel, null)
                    .create();

            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.parseColor("#1565C0"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.parseColor("#F44336"));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setTextColor(Color.parseColor("#757575"));
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(Color.WHITE));
            }

        } catch (Exception e) {
            android.util.Log.e("SWIMCRASH", "showSaveDialog: " + e);
            resetAll();
        }
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this,
                    "com.swimtimer.app.fileprovider", photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Errore fotocamera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "SWIM_" + timeStamp;
        File storageDir = new File(getFilesDir(), "photos");
        if (!storageDir.exists()) storageDir.mkdirs();
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void resetAll() {
        elapsedTime = 0; lastLapTime = 0; isRunning = false;
        currentPhotoPath = null;
        dialogPhotoPreview = null;
        dialogPhotoLabel = null;
        handler.removeCallbacks(timerRunnable);
        laps.clear();
        lapAdapter.notifyDataSetChanged();
        binding.tvTime.setText("00:00.00");
        binding.tvCurrentLap.setText(getString(R.string.current_lap) + " 00:00.00");
        binding.swimmerView.setRunning(false);
        binding.waveView.setRunning(false);
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
            vibrator.vibrate(VibrationEffect.createOneShot(150,
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
