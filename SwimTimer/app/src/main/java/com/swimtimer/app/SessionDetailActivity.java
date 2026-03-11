package com.swimtimer.app;

import android.content.Intent;
import android.graphics.BitmapFactory;
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
            ivPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        }

        // Laps
        List<Long> laps = session.getLaps();
        RecyclerView rv = findViewById(R.id.rvLaps);
        TextView tvNoLaps = findViewById(R.id.tvNoLaps);
        if (laps == null || laps.isEmpty()) {
            tvNoLaps.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvNoLaps.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new DetailLapAdapter(laps));
        }

        // Pulsante rinomina
        ((MaterialButton) findViewById(R.id.btnRename)).setOnClickListener(v -> showRenameDialog());

        // Pulsante condividi
        ((MaterialButton) findViewById(R.id.btnShare)).setOnClickListener(v -> shareSession());
    }

    private void showRenameDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(session.getName());
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Rinomina gara")
                .setView(input)
                .setPositiveButton("Salva", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        SessionStorage.renameSession(this, session.getId(), newName);
                        session = SessionStorage.loadById(this, session.getId());
                        ((TextView) findViewById(R.id.tvName)).setText(session.getName());
                        if (getSupportActionBar() != null)
                            getSupportActionBar().setTitle(session.getName());
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void shareSession() {
        // Costruisci il testo
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

        if (hasPhoto) {
            // Condividi testo + foto
            Uri photoUri = FileProvider.getUriForFile(this,
                    "com.swimtimer.app.fileprovider", new File(photoPath));

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            intent.putExtra(Intent.EXTRA_STREAM, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Prova WhatsApp prima
            intent.setPackage("com.whatsapp");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                intent.setPackage(null);
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
            }
        } else {
            // Solo testo
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            intent.setPackage("com.whatsapp");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                intent.setPackage(null);
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
