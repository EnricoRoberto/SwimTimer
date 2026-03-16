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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

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

    // Setup opzionale
    private String pendingAthleteName = "";
    private String pendingSpecialty   = "";
    private boolean setupDone         = false;
    private int currentRecordType     = SessionStorage.RECORD_NONE;

    // Foto
    private String currentPhotoPath   = null;
    private Uri photoUri               = null;
    private ImageView dialogPhotoPreview = null;
    private TextView dialogPhotoLabel   = null;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentPhotoPath != null) {
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
                else Toast.makeText(this, "Permesso fotocamera negato",
                        Toast.LENGTH_SHORT).show();
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
            binding.btnLap.setOnClickListener(v -> {
                if (isRunning) { vibrate(); recordLap(); }
            });
            binding.btnReset.setOnClickListener(v -> { vibrate(); onResetPressed(); });

            // Pulsante setup opzionale
            binding.btnSetup.setOnClickListener(v -> showSetupDialog());

            updateUI();
            updateSetupBar();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Errore onCreate")
                    .setMessage(e.toString())
                    .setPositiveButton("OK", null).show();
        }
    }

    /** Aggiorna il pulsante setup in base allo stato */
    private void updateSetupBar() {
        if (setupDone) {
            String label = pendingAthleteName.isEmpty() ?
                    pendingSpecialty :
                    pendingAthleteName + " — " + pendingSpecialty;
            binding.btnSetup.setText("✅  " + label);
            binding.btnSetup.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#4CAF50")));
            binding.btnSetup.setTextColor(Color.WHITE);
        } else {
            binding.btnSetup.setText("⚙  Imposta atleta e specialità");
            binding.btnSetup.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#FFD600")));
            binding.btnSetup.setTextColor(Color.parseColor("#212121"));
        }
        // Nascondi il pulsante durante il cronometraggio
        binding.btnSetup.setVisibility(isRunning ? View.GONE : View.VISIBLE);
    }

    /** Dialog setup opzionale — non blocca, chiamabile in qualsiasi momento */
    private void showSetupDialog() {
        boolean isDark    = themeManager.getCurrentTheme() == ThemeManager.THEME_DARK;
        int bgColor       = isDark ? Color.parseColor("#1A2A3A") : Color.WHITE;
        int textPrimary   = isDark ? Color.WHITE : Color.parseColor("#212121");
        int textSecondary = isDark ? Color.parseColor("#AACCE0") : Color.parseColor("#555555");
        int dividerColor  = isDark ? Color.parseColor("#2A4A6A") : Color.parseColor("#E0E0E0");
        int previewColor  = isDark ? Color.parseColor("#64B5F6") : Color.parseColor("#1565C0");

        View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);
        dv.findViewById(R.id.dialogContainer).setBackgroundColor(bgColor);

        // Nascondi sezione foto
        dv.findViewById(R.id.labelPhoto).setVisibility(View.GONE);
        dv.findViewById(R.id.btnTakePhoto).setVisibility(View.GONE);
        dv.findViewById(R.id.tvPhotoLabel).setVisibility(View.GONE);
        dv.findViewById(R.id.ivPhotoPreview).setVisibility(View.GONE);
        dv.findViewById(R.id.divider2).setVisibility(View.GONE);

        ((TextView) dv.findViewById(R.id.labelName)).setTextColor(textPrimary);
        ((TextView) dv.findViewById(R.id.labelSpecialty)).setTextColor(textPrimary);
        ((TextView) dv.findViewById(R.id.labelDistance)).setTextColor(textSecondary);
        ((TextView) dv.findViewById(R.id.labelStyle)).setTextColor(textSecondary);
        dv.findViewById(R.id.divider1).setBackgroundColor(dividerColor);

        com.google.android.material.textfield.TextInputEditText etName =
                dv.findViewById(R.id.etSessionName);
        etName.setTextColor(textPrimary);
        etName.setHintTextColor(textSecondary);
        if (!pendingAthleteName.isEmpty()) {
            etName.setText(pendingAthleteName);
            etName.setSelection(pendingAthleteName.length());
        }

        android.widget.TextView tvPreview = dv.findViewById(R.id.tvSpecialtyPreview);
        tvPreview.setTextColor(previewColor);

        android.widget.Spinner spinnerDistance = dv.findViewById(R.id.spinnerDistance);
        android.widget.Spinner spinnerStyle    = dv.findViewById(R.id.spinnerStyle);
        final String[] distances = {"25m", "50m", "100m", "200m"};
        final String[] styles    = {"Dorso", "Farfalla", "Rana", "Stile Libero", "Misti"};

        spinnerDistance.setAdapter(makeSpinnerAdapter(distances, textPrimary, bgColor));
        spinnerStyle.setAdapter(makeSpinnerAdapter(styles, textPrimary, bgColor));

        // Preseleziona ultima specialità
        if (!pendingSpecialty.isEmpty()) {
            for (int i = 0; i < distances.length; i++) {
                if (pendingSpecialty.startsWith(distances[i])) {
                    spinnerDistance.setSelection(i); break;
                }
            }
            for (int i = 0; i < styles.length; i++) {
                if (pendingSpecialty.contains(styles[i])) {
                    spinnerStyle.setSelection(i); break;
                }
            }
        }

        android.widget.AdapterView.OnItemSelectedListener previewListener =
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            android.widget.AdapterView<?> p, View v, int pos, long id) {
                        tvPreview.setText("➡ "
                                + distances[spinnerDistance.getSelectedItemPosition()]
                                + " " + styles[spinnerStyle.getSelectedItemPosition()]);
                    }
                    @Override public void onNothingSelected(
                            android.widget.AdapterView<?> p) {}
                };
        spinnerDistance.setOnItemSelectedListener(previewListener);
        spinnerStyle.setOnItemSelectedListener(previewListener);
        tvPreview.setText("➡ " + distances[spinnerDistance.getSelectedItemPosition()]
                + " " + styles[spinnerStyle.getSelectedItemPosition()]);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("⚙ Setup gara")
                .setView(dv)
                .setPositiveButton("Conferma", (d, w) -> {
                    pendingAthleteName = etName.getText() != null ?
                            etName.getText().toString().trim() : "";
                    pendingSpecialty = distances[spinnerDistance.getSelectedItemPosition()]
                            + " " + styles[spinnerStyle.getSelectedItemPosition()];
                    setupDone = true;
                    updateSetupBar();
                    // Se il cronometro sta girando aggiorna subito i record
                    if (isRunning && !laps.isEmpty()) {
                        updateRecordBadge();
                    }
                })
                .setNegativeButton("Annulla", null)
                .create();

        dialog.show();
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.parseColor("#1565C0"));
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.parseColor("#757575"));
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(bgColor));
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
        updateSetupBar();
    }

    private void recordLap() {
        long total = elapsedTime + (System.currentTimeMillis() - startTime);
        long lapTime = total - lastLapTime;
        laps.add(0, lapTime);
        lastLapTime = total;
        lapAdapter.notifyItemInserted(0);
        binding.rvLaps.scrollToPosition(0);
        binding.swimmerView.doTumble();

        // Aggiorna icona record in tempo reale solo se setup fatto
        if (setupDone) updateRecordBadge();
    }

    /** Ricalcola e aggiorna l'icona record sulla vasca più veloce */
    private void updateRecordBadge() {
        if (laps.isEmpty() || !setupDone) return;
        long fastest = Long.MAX_VALUE;
        for (Long l : laps) if (l < fastest) fastest = l;
        currentRecordType = SessionStorage.checkRecord(
                this, pendingSpecialty, fastest, null);
        lapAdapter.setRecordType(currentRecordType);
    }

    private void onResetPressed() {
        if (isRunning) {
            elapsedTime += System.currentTimeMillis() - startTime;
            isRunning = false;
            handler.removeCallbacks(timerRunnable);
            binding.swimmerView.setRunning(false);
            binding.waveView.setRunning(false);
            updateUI();
            updateSetupBar();
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
            boolean isDark    = themeManager.getCurrentTheme() == ThemeManager.THEME_DARK;
            int bgColor       = isDark ? Color.parseColor("#1A2A3A") : Color.WHITE;
            int textPrimary   = isDark ? Color.WHITE : Color.parseColor("#212121");
            int textSecondary = isDark ? Color.parseColor("#AACCE0") : Color.parseColor("#555555");
            int dividerColor  = isDark ? Color.parseColor("#2A4A6A") : Color.parseColor("#E0E0E0");
            int previewColor  = isDark ? Color.parseColor("#64B5F6") : Color.parseColor("#1565C0");

            View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);
            dv.findViewById(R.id.dialogContainer).setBackgroundColor(bgColor);
            ((TextView) dv.findViewById(R.id.labelName)).setTextColor(textPrimary);
            ((TextView) dv.findViewById(R.id.labelSpecialty)).setTextColor(textPrimary);
            ((TextView) dv.findViewById(R.id.labelPhoto)).setTextColor(textPrimary);
            ((TextView) dv.findViewById(R.id.labelDistance)).setTextColor(textSecondary);
            ((TextView) dv.findViewById(R.id.labelStyle)).setTextColor(textSecondary);
            dv.findViewById(R.id.divider1).setBackgroundColor(dividerColor);
            dv.findViewById(R.id.divider2).setBackgroundColor(dividerColor);

            com.google.android.material.textfield.TextInputEditText etName =
                    dv.findViewById(R.id.etSessionName);
            etName.setTextColor(textPrimary);
            etName.setHintTextColor(textSecondary);

            // Precompila con setup se fatto
            if (!pendingAthleteName.isEmpty()) {
                etName.setText(pendingAthleteName);
                etName.setSelection(pendingAthleteName.length());
            }

            android.widget.TextView tvPreview = dv.findViewById(R.id.tvSpecialtyPreview);
            tvPreview.setTextColor(previewColor);
            ((TextView) dv.findViewById(R.id.tvPhotoLabel)).setTextColor(textSecondary);

            android.widget.Spinner spinnerDistance = dv.findViewById(R.id.spinnerDistance);
            android.widget.Spinner spinnerStyle    = dv.findViewById(R.id.spinnerStyle);
            final String[] distances = {"25m", "50m", "100m", "200m"};
            final String[] styles    = {"Dorso", "Farfalla", "Rana", "Stile Libero", "Misti"};

            spinnerDistance.setAdapter(makeSpinnerAdapter(distances, textPrimary, bgColor));
            spinnerStyle.setAdapter(makeSpinnerAdapter(styles, textPrimary, bgColor));

            // Preseleziona specialità da setup
            if (!pendingSpecialty.isEmpty()) {
                for (int i = 0; i < distances.length; i++) {
                    if (pendingSpecialty.startsWith(distances[i])) {
                        spinnerDistance.setSelection(i); break;
                    }
                }
                for (int i = 0; i < styles.length; i++) {
                    if (pendingSpecialty.contains(styles[i])) {
                        spinnerStyle.setSelection(i); break;
                    }
                }
            }

            android.widget.AdapterView.OnItemSelectedListener previewListener =
                    new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(
                                android.widget.AdapterView<?> p, View v, int pos, long id) {
                            String dist    = distances[spinnerDistance.getSelectedItemPosition()];
                            String style   = styles[spinnerStyle.getSelectedItemPosition()];
                            String specialty = dist + " " + style;
                            if (!laps.isEmpty()) {
                                long fastest = Long.MAX_VALUE;
                                for (Long l : laps) if (l < fastest) fastest = l;
                                int record = SessionStorage.checkRecord(
                                        MainActivity.this, specialty, fastest, null);
                                lapAdapter.setRecordType(record);
                                switch (record) {
                                    case SessionStorage.RECORD_ABSOLUTE:
                                        tvPreview.setText("➡ " + specialty + "  🥇 Record!");
                                        break;
                                    case SessionStorage.RECORD_IMPROVED:
                                        tvPreview.setText("➡ " + specialty + "  📈 Migliorato!");
                                        break;
                                    case SessionStorage.RECORD_BOTH:
                                        tvPreview.setText("➡ " + specialty + "  🥇📈 Record+Migliorato!");
                                        break;
                                    default:
                                        tvPreview.setText("➡ " + specialty);
                                }
                            } else {
                                tvPreview.setText("➡ " + dist + " " + style);
                            }
                        }
                        @Override public void onNothingSelected(
                                android.widget.AdapterView<?> p) {}
                    };
            spinnerDistance.setOnItemSelectedListener(previewListener);
            spinnerStyle.setOnItemSelectedListener(previewListener);

            // Foto
            com.google.android.material.button.MaterialButton btnPhoto =
                    dv.findViewById(R.id.btnTakePhoto);
            dialogPhotoPreview = dv.findViewById(R.id.ivPhotoPreview);
            dialogPhotoLabel   = dv.findViewById(R.id.tvPhotoLabel);
            btnPhoto.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    launchCamera();
                } else {
                    requestCameraPermission.launch(Manifest.permission.CAMERA);
                }
            });

            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("💾 Salva Gara")
                    .setView(dv)
                    .setPositiveButton(R.string.save, (d, w) -> {
                        String athleteName = etName.getText() != null ?
                                etName.getText().toString().trim() : "";
                        String dist  = distances[spinnerDistance.getSelectedItemPosition()];
                        String style = styles[spinnerStyle.getSelectedItemPosition()];
                        String sessionName = athleteName.isEmpty() ?
                                dist + " " + style :
                                athleteName + " — " + dist + " " + style;
                        List<Long> savedLaps = new ArrayList<>(laps);
                        Collections.reverse(savedLaps);
                        SessionData session = new SessionData(sessionName,
                                System.currentTimeMillis(), elapsedTime, savedLaps);
                        if (currentPhotoPath != null) session.setPhotoPath(currentPhotoPath);
                        SessionStorage.saveSession(this, session);
                        Toast.makeText(this, R.string.session_saved,
                                Toast.LENGTH_SHORT).show();
                        resetAll();
                    })
                    .setNegativeButton(R.string.discard, (d, w) -> {
                        currentPhotoPath = null; resetAll();
                    })
                    .setNeutralButton(R.string.cancel, null)
                    .create();

            dialog.show();
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.parseColor("#1565C0"));
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.parseColor("#F44336"));
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)
                    .setTextColor(Color.parseColor("#757575"));
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(bgColor));

        } catch (Exception e) {
            android.util.Log.e("SWIMCRASH", "showSaveDialog: " + e);
            resetAll();
        }
    }

    private android.widget.ArrayAdapter<String> makeSpinnerAdapter(
            String[] items, int textColor, int bgColor) {
        return new android.widget.ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int pos, View convertView, android.view.ViewGroup parent) {
                android.widget.TextView tv =
                        (android.widget.TextView) super.getView(pos, convertView, parent);
                tv.setTextColor(textColor);
                tv.setBackgroundColor(bgColor);
                return tv;
            }
            @Override
            public View getDropDownView(int pos, View convertView,
                    android.view.ViewGroup parent) {
                android.widget.TextView tv =
                        (android.widget.TextView) super.getDropDownView(
                                pos, convertView, parent);
                tv.setTextColor(textColor);
                tv.setBackgroundColor(bgColor);
                tv.setPadding(32, 24, 32, 24);
                return tv;
            }
        };
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
        File storageDir = new File(getFilesDir(), "photos");
        if (!storageDir.exists()) storageDir.mkdirs();
        File image = File.createTempFile("SWIM_" + timeStamp, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void resetAll() {
        elapsedTime = 0; lastLapTime = 0; isRunning = false;
        currentPhotoPath = null;
        currentRecordType = SessionStorage.RECORD_NONE;
        setupDone = false;
        pendingAthleteName = "";
        pendingSpecialty = "";
        dialogPhotoPreview = null;
        dialogPhotoLabel = null;
        handler.removeCallbacks(timerRunnable);
        laps.clear();
        lapAdapter.notifyDataSetChanged();
        lapAdapter.setRecordType(SessionStorage.RECORD_NONE);
        binding.tvTime.setText("00:00.00");
        binding.tvCurrentLap.setText(getString(R.string.current_lap) + " 00:00.00");
        binding.swimmerView.setRunning(false);
        binding.waveView.setRunning(false);
        updateUI();
        updateSetupBar();
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
