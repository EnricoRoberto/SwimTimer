package com.swimtimer.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
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

    // Microfono / riconoscimento sirena
    private AudioRecord audioRecord;
    private boolean isListening = false;
    private Thread listenThread;

    // Parametri rilevamento sirena
    private static final int SAMPLE_RATE      = 44100;
    private static final int CHANNEL_CONFIG   = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT     = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE      = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 4;
    // Frequenze sirena gara nuoto — suono grave 80-500 Hz
    private static final double FREQ_MIN      = 80.0;
    private static final double FREQ_MAX      = 500.0;
    // Soglia volume bassa per captare a distanza
    private static final double VOLUME_THRESHOLD = 800.0;
    // Durata minima rilevamento (ms)
    private static final long DETECTION_DURATION = 400;

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

    private final ActivityResultLauncher<String> requestMicPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startListening();
                else Toast.makeText(this, "Permesso microfono negato",
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

    // Lampeggio pulsante microfono
    private final Runnable blinkRunnable = new Runnable() {
        private boolean state = false;
        @Override public void run() {
            if (!isListening) return;
            state = !state;
            binding.btnAutoStart.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            state ? Color.parseColor("#F44336")
                                  : Color.parseColor("#546E7A")));
            handler.postDelayed(this, 500);
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
            binding.btnSetup.setOnClickListener(v -> showSetupDialog());
            binding.btnAutoStart.setOnClickListener(v -> { vibrate(); toggleListening(); });

            handleImportIntent(getIntent());
            updateUI();
            updateSetupBar();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Errore onCreate")
                    .setMessage(e.toString())
                    .setPositiveButton("OK", null).show();
        }
    }

    // ── MICROFONO ──────────────────────────────────────────────────────────────

    private void toggleListening() {
        if (isRunning) {
            Toast.makeText(this, "Ferma il cronometro prima", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isListening) {
            stopListening();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    private void startListening() {
        if (isListening) return;
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            audioRecord.startRecording();
            isListening = true;
            updateAutoStartButton();
            handler.post(blinkRunnable);
            Toast.makeText(this, "🎤 In ascolto sirena...", Toast.LENGTH_SHORT).show();

            listenThread = new Thread(() -> {
                short[] buffer = new short[BUFFER_SIZE / 2];
                long sirenStartTime = -1;

                while (isListening) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read <= 0) continue;

                    // Calcola volume RMS
                    double rms = 0;
                    for (int i = 0; i < read; i++) rms += buffer[i] * buffer[i];
                    rms = Math.sqrt(rms / read);

                    if (rms < VOLUME_THRESHOLD) {
                        sirenStartTime = -1;
                        continue;
                    }

                    // Analisi FFT per verificare frequenza dominante
                    double dominantFreq = getDominantFrequency(buffer, read);
                    boolean isInSirenRange = dominantFreq >= FREQ_MIN
                            && dominantFreq <= FREQ_MAX;

                    if (isInSirenRange) {
                        if (sirenStartTime == -1) {
                            sirenStartTime = System.currentTimeMillis();
                        } else if (System.currentTimeMillis() - sirenStartTime
                                >= DETECTION_DURATION) {
                            // Sirena rilevata!
                            runOnUiThread(() -> {
                                stopListening();
                                startTimerFromSiren();
                            });
                            break;
                        }
                    } else {
                        sirenStartTime = -1;
                    }
                }
            });
            listenThread.start();

        } catch (Exception e) {
            Toast.makeText(this, "Errore microfono: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            isListening = false;
            updateAutoStartButton();
        }
    }

    private void stopListening() {
        isListening = false;
        handler.removeCallbacks(blinkRunnable);
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception ignored) {}
            audioRecord = null;
        }
        updateAutoStartButton();
    }

    private void startTimerFromSiren() {
        if (isRunning) return;
        vibrate();
        Toast.makeText(this, "🚨 Sirena rilevata — partenza!", Toast.LENGTH_SHORT).show();
        startTime = System.currentTimeMillis();
        isRunning = true;
        handler.post(timerRunnable);
        binding.swimmerView.setRunning(true);
        binding.waveView.setRunning(true);
        updateUI();
        updateSetupBar();
    }

    /** FFT semplificata per trovare la frequenza dominante */
    private double getDominantFrequency(short[] buffer, int length) {
        // Usa un campione ridotto per efficienza
        int n = Math.min(length, 2048);
        double[] real = new double[n];
        double[] imag = new double[n];

        // Applica finestra di Hanning
        for (int i = 0; i < n; i++) {
            double window = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
            real[i] = buffer[i] * window;
            imag[i] = 0;
        }

        // FFT iterativa (Cooley-Tukey)
        fft(real, imag, n);

        // Trova il bin con ampiezza massima (escludi DC)
        int maxBin = 1;
        double maxAmp = 0;
        for (int i = 1; i < n / 2; i++) {
            double amp = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            if (amp > maxAmp) {
                maxAmp = amp;
                maxBin = i;
            }
        }

        return (double) maxBin * SAMPLE_RATE / n;
    }

    /** FFT di Cooley-Tukey — opera su potenze di 2 */
    private void fft(double[] re, double[] im, int n) {
        // Bit reversal
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        // FFT
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            double wRe = Math.cos(ang), wIm = Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                double curRe = 1, curIm = 0;
                for (int j = 0; j < len / 2; j++) {
                    double uRe = re[i+j], uIm = im[i+j];
                    double vRe = re[i+j+len/2]*curRe - im[i+j+len/2]*curIm;
                    double vIm = re[i+j+len/2]*curIm + im[i+j+len/2]*curRe;
                    re[i+j] = uRe+vRe; im[i+j] = uIm+vIm;
                    re[i+j+len/2] = uRe-vRe; im[i+j+len/2] = uIm-vIm;
                    double newRe = curRe*wRe - curIm*wIm;
                    curIm = curRe*wIm + curIm*wRe;
                    curRe = newRe;
                }
            }
        }
    }

    private void updateAutoStartButton() {
        runOnUiThread(() -> {
            if (isListening) {
                binding.btnAutoStart.setText("⏹");
                binding.btnAutoStart.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#F44336")));
            } else {
                binding.btnAutoStart.setText("🎤");
                binding.btnAutoStart.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#546E7A")));
            }
        });
    }

    // ── TIMER ──────────────────────────────────────────────────────────────────

    private void toggleTimer() {
        if (isRunning) {
            elapsedTime += System.currentTimeMillis() - startTime;
            isRunning = false;
            handler.removeCallbacks(timerRunnable);
            binding.swimmerView.setRunning(false);
            binding.waveView.setRunning(false);
        } else {
            if (isListening) stopListening();
            startTime = System.currentTimeMillis();
            isRunning = true;
            handler.post(timerRunnable);
            binding.swimmerView.setRunning(true);
            binding.waveView.setRunning(true);
        }
        updateUI();
        updateSetupBar();
    }

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
        binding.btnSetup.setVisibility(isRunning ? View.GONE : View.VISIBLE);
    }

    private void showSetupDialog() {
        boolean isDark    = themeManager.getCurrentTheme() == ThemeManager.THEME_DARK;
        int bgColor       = isDark ? Color.parseColor("#1A2A3A") : Color.WHITE;
        int textPrimary   = isDark ? Color.WHITE : Color.parseColor("#212121");
        int textSecondary = isDark ? Color.parseColor("#AACCE0") : Color.parseColor("#555555");
        int dividerColor  = isDark ? Color.parseColor("#2A4A6A") : Color.parseColor("#E0E0E0");
        int previewColor  = isDark ? Color.parseColor("#64B5F6") : Color.parseColor("#1565C0");

        View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);
        dv.findViewById(R.id.dialogContainer).setBackgroundColor(bgColor);
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
                    if (isRunning && !laps.isEmpty()) updateRecordBadge();
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

    private void recordLap() {
        long total = elapsedTime + (System.currentTimeMillis() - startTime);
        long lapTime = total - lastLapTime;
        laps.add(0, lapTime);
        lastLapTime = total;
        lapAdapter.notifyItemInserted(0);
        binding.rvLaps.scrollToPosition(0);
        binding.swimmerView.doTumble();
        if (setupDone) updateRecordBadge();
    }

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
            binding.btnAutoStart.setEnabled(false);
        } else {
            binding.btnStartStop.setText(R.string.start);
            binding.btnStartStop.setIconResource(R.drawable.ic_play);
            binding.btnLap.setEnabled(false);
            binding.btnReset.setEnabled(elapsedTime > 0);
            binding.btnAutoStart.setEnabled(true);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleImportIntent(intent);
    }

    private void handleImportIntent(Intent intent) {
        if (intent == null) return;
        android.net.Uri uri = intent.getData();
        if (uri == null || !"swimtimer".equals(uri.getScheme())) return;
        if (!"import".equals(uri.getHost())) return;
        String encodedData = uri.getQueryParameter("data");
        if (encodedData == null || encodedData.isEmpty()) return;
        try {
            byte[] decoded = android.util.Base64.decode(
                    encodedData, android.util.Base64.URL_SAFE);
            String json = new String(decoded);
            org.json.JSONObject obj = new org.json.JSONObject(json);
            String name      = obj.getString("name");
            long date        = obj.getLong("date");
            long totalTime   = obj.getLong("totalTime");
            List<Long> laps  = new ArrayList<>();
            org.json.JSONArray lapsArr = obj.getJSONArray("laps");
            for (int i = 0; i < lapsArr.length(); i++) laps.add(lapsArr.getLong(i));
            SessionData imported = new SessionData(name, date, totalTime, laps);
            imported.setImported(true);

            android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("📥 Importa sessione")
                    .setMessage("Vuoi importare la sessione:\n\n"
                            + "🏊 " + name + "\n"
                            + "⏱ " + formatTime(totalTime) + "\n"
                            + "🏁 " + laps.size() + " vasche")
                    .setPositiveButton("Importa", (d, w) -> {
                        SessionStorage.saveSession(this, imported);
                        Toast.makeText(this, "✅ Sessione importata!",
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Annulla", null)
                    .create();

            confirmDialog.show();
            if (confirmDialog.getWindow() != null)
                confirmDialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(Color.WHITE));
            confirmDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.parseColor("#1565C0"));
            confirmDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.parseColor("#757575"));
            android.widget.TextView messageView =
                    confirmDialog.findViewById(android.R.id.message);
            if (messageView != null)
                messageView.setTextColor(Color.parseColor("#212121"));
            android.widget.TextView titleView =
                    confirmDialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (titleView != null)
                titleView.setTextColor(Color.parseColor("#0D3B5E"));

        } catch (Exception e) {
            Toast.makeText(this, "Errore nel link di importazione",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timerRunnable);
        handler.removeCallbacks(blinkRunnable);
        stopListening();
    }
}
