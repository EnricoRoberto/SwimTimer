package com.swimtimer.app;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class SessionDetailActivity extends AppCompatActivity {

    private SessionData session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String sessionId = getIntent().getStringExtra("session_id");
        session = SessionStorage.loadById(this, sessionId);
        if (session == null) { finish(); return; }

        getSupportActionBar().setTitle(session.getName());
        ((TextView) findViewById(R.id.tvName)).setText(session.getName());
        ((TextView) findViewById(R.id.tvDate)).setText(
                DateFormat.getDateTimeInstance().format(new Date(session.getDate())));
        ((TextView) findViewById(R.id.tvTotal)).setText(
                MainActivity.formatTime(session.getTotalTime()));

        // Foto
        ImageView ivPhoto = findViewById(R.id.ivSessionPhoto);
        String photoPath = session.getPhotoPath();
        if (photoPath != null && !photoPath.isEmpty() && new File(photoPath).exists()) {
            ivPhoto.setVisibility(View.VISIBLE);
            ivPhoto.setImageBitmap(SessionListAdapter.loadCorrectlyOrientedBitmap(photoPath));
        }

        // Laps
        List<Long> laps = session.getLaps();
        RecyclerView rv = findViewById(R.id.rvLaps);
        TextView tvNoLaps = findViewById(R.id.tvNoLaps);
        if (laps == null || laps.isEmpty()) {
            tvNoLaps.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            rv.setVisibility(View.VISIBLE);
            rv.setLayoutManager(new LinearLayoutManager(this));
            DetailLapAdapter detailAdapter = new DetailLapAdapter(laps);

            // Controlla record per questa sessione
            String specialty = SessionStorage.extractSpecialty(session.getName());
            if (!specialty.isEmpty() && !laps.isEmpty()) {
                long fastest = Long.MAX_VALUE;
                for (Long l : laps) if (l < fastest) fastest = l;
                int record = SessionStorage.checkRecord(
                        this, specialty, fastest, session.getId());
                detailAdapter.setRecordType(record);
            }

            rv.setAdapter(detailAdapter);
        }

        ((MaterialButton) findViewById(R.id.btnRename))
                .setOnClickListener(v -> showRenameDialog());
        ((MaterialButton) findViewById(R.id.btnShare))
                .setOnClickListener(v -> shareSession());
    }

    private void showRenameDialog() {
        int currentTheme = ThemeManager.getInstance(this).getCurrentTheme();
        boolean isDark   = currentTheme == ThemeManager.THEME_DARK;
        int bgColor      = isDark ? Color.parseColor("#1A2A3A") : Color.WHITE;
        int textPrimary  = isDark ? Color.WHITE : Color.parseColor("#212121");
        int textSecondary= isDark ? Color.parseColor("#AACCE0") : Color.parseColor("#555555");
        int dividerColor = isDark ? Color.parseColor("#2A4A6A") : Color.parseColor("#E0E0E0");
        int previewColor = isDark ? Color.parseColor("#64B5F6") : Color.parseColor("#1565C0");

        View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);

        // Sfondo container
        dv.findViewById(R.id.dialogContainer).setBackgroundColor(bgColor);

        // Colori label
        setTextColor(dv, R.id.labelName, textPrimary);
        setTextColor(dv, R.id.labelSpecialty, textPrimary);
        setTextColor(dv, R.id.labelDistance, textSecondary);
        setTextColor(dv, R.id.labelStyle, textSecondary);

        // Dividers
        dv.findViewById(R.id.divider1).setBackgroundColor(dividerColor);

        // Nascondi sezione foto
        dv.findViewById(R.id.labelPhoto).setVisibility(View.GONE);
        dv.findViewById(R.id.btnTakePhoto).setVisibility(View.GONE);
        dv.findViewById(R.id.tvPhotoLabel).setVisibility(View.GONE);
        dv.findViewById(R.id.ivPhotoPreview).setVisibility(View.GONE);
        dv.findViewById(R.id.divider2).setVisibility(View.GONE);

        // EditText nome
        com.google.android.material.textfield.TextInputEditText etName =
                dv.findViewById(R.id.etSessionName);
        etName.setTextColor(textPrimary);
        etName.setHintTextColor(textSecondary);

        // Preview
        android.widget.TextView tvPreview = dv.findViewById(R.id.tvSpecialtyPreview);
        tvPreview.setTextColor(previewColor);

        // Precompila nome atleta separando "Nome — Specialità"
        String currentName = session.getName();
        String prefillName = currentName;
        int sepIdx = currentName.indexOf(" — ");
        if (sepIdx > 0) prefillName = currentName.substring(0, sepIdx);
        etName.setText(prefillName);
        etName.setSelection(prefillName.length());

        // Spinner
        android.widget.Spinner spinnerDistance = dv.findViewById(R.id.spinnerDistance);
        android.widget.Spinner spinnerStyle    = dv.findViewById(R.id.spinnerStyle);
        String[] distances = {"25m", "50m", "100m", "200m"};
        String[] styles    = {"Dorso", "Farfalla", "Rana", "Stile Libero", "Misti"};

        spinnerDistance.setAdapter(makeSpinnerAdapter(distances, textPrimary, bgColor));
        spinnerStyle.setAdapter(makeSpinnerAdapter(styles, textPrimary, bgColor));

        // Preseleziona distanza e stile dal nome corrente
        if (sepIdx > 0) {
            String specialty = currentName.substring(sepIdx + 3);
            for (int i = 0; i < distances.length; i++) {
                if (specialty.startsWith(distances[i])) {
                    spinnerDistance.setSelection(i); break;
                }
            }
            for (int i = 0; i < styles.length; i++) {
                if (specialty.contains(styles[i])) {
                    spinnerStyle.setSelection(i); break;
                }
            }
        }

        // Aggiorna preview quando cambia selezione
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
        // Forza aggiornamento iniziale preview
        tvPreview.setText("➡ "
                + distances[spinnerDistance.getSelectedItemPosition()]
                + " " + styles[spinnerStyle.getSelectedItemPosition()]);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Rinomina gara")
                .setView(dv)
                .setPositiveButton("Salva", (d, w) -> {
                    String athleteName = etName.getText() != null ?
                            etName.getText().toString().trim() : "";
                    String dist  = distances[spinnerDistance.getSelectedItemPosition()];
                    String style = styles[spinnerStyle.getSelectedItemPosition()];
                    String newName = athleteName.isEmpty() ?
                            dist + " " + style :
                            athleteName + " — " + dist + " " + style;
                    SessionStorage.renameSession(this, session.getId(), newName);
                    session = SessionStorage.loadById(this, session.getId());
                    ((TextView) findViewById(R.id.tvName)).setText(session.getName());
                    if (getSupportActionBar() != null)
                        getSupportActionBar().setTitle(session.getName());
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

    private void setTextColor(View root, int viewId, int color) {
        ((TextView) root.findViewById(viewId)).setTextColor(color);
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

    private void shareSession() {
        StringBuilder sb = new StringBuilder();
        sb.append("🏊 ").append(session.getName()).append("\n");
        sb.append("📅 ").append(DateFormat.getDateTimeInstance()
                .format(new Date(session.getDate()))).append("\n");
        sb.append("⏱ Tempo totale: ")
                .append(MainActivity.formatTime(session.getTotalTime())).append("\n");

        List<Long> laps = session.getLaps();
        if (laps != null && !laps.isEmpty()) {
            sb.append("\n🏁 Parziali:\n");
            long fastest = Long.MAX_VALUE, slowest = 0;
            for (Long l : laps) {
                if (l < fastest) fastest = l;
                if (l > slowest) slowest = l;
            }
            for (int i = 0; i < laps.size(); i++) {
                long t = laps.get(i);
                String tag = t == fastest ? " 🟢" : t == slowest ? " 🔴" : "";
                sb.append("  Vasca ").append(i + 1).append(": ")
                        .append(MainActivity.formatTime(t)).append(tag).append("\n");
            }
        }
        sb.append("\n📲 SwimTimer App");

        String photoPath = session.getPhotoPath();
        boolean hasPhoto = photoPath != null && !photoPath.isEmpty()
                && new File(photoPath).exists();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        if (hasPhoto) {
            Uri photoUri = FileProvider.getUriForFile(this,
                    "com.swimtimer.app.fileprovider", new File(photoPath));
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setType("text/plain");
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
