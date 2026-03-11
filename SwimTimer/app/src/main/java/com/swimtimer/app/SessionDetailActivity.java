package com.swimtimer.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.swimtimer.app.databinding.ActivitySessionDetailBinding;
import java.text.DateFormat;
import java.util.*;

public class SessionDetailActivity extends AppCompatActivity {
    private ActivitySessionDetailBinding binding;
    private SessionData session;

    @Override protected void onCreate(Bundle s) {
        ThemeManager.getInstance(this).applyTheme(this);
        super.onCreate(s);
        binding = ActivitySessionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String sid = getIntent().getStringExtra("sid");
        for (SessionData sd : SessionStorage.getSessions(this))
            if (sd.getId().equals(sid)) { session = sd; break; }

        if (session == null) { finish(); return; }
        populate();

        // Pulsante Rinomina
        binding.btnRename.setOnClickListener(v -> {
            View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);
            com.google.android.material.textfield.TextInputEditText et =
                    dv.findViewById(R.id.etSessionName);
            et.setText(session.getName());
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.rename).setView(dv)
                    .setPositiveButton(R.string.save, (d, w) -> {
                        String n = et.getText() != null ?
                                et.getText().toString().trim() : "";
                        if (!n.isEmpty()) {
                            session.setName(n);
                            SessionStorage.updateSession(this, session);
                            populate();
                        }
                    }).setNegativeButton(R.string.cancel, null).show();
        });

        // Pulsante Condividi
        binding.btnShare.setOnClickListener(v -> shareSession());
    }

    private void shareSession() {
        StringBuilder sb = new StringBuilder();
        sb.append("🏊 SwimTimer\n");
        sb.append("━━━━━━━━━━━━━━━━━\n");
        sb.append("🏁 Gara: ").append(session.getName()).append("\n");
        sb.append("📅 ").append(DateFormat.getDateTimeInstance()
                .format(new Date(session.getDate()))).append("\n");
        sb.append("⏱ Tempo totale: ")
                .append(MainActivity.formatTime(session.getTotalTime())).append("\n");

        List<Long> laps = session.getLaps();
        if (laps != null && !laps.isEmpty()) {
            sb.append("━━━━━━━━━━━━━━━━━\n");
            sb.append("🔢 Vasche:\n");

            long min = Long.MAX_VALUE;
            int minIndex = 0;
            for (int i = 0; i < laps.size(); i++) {
                sb.append("  Vasca ").append(i + 1).append(": ")
                        .append(MainActivity.formatTime(laps.get(i))).append("\n");
                if (laps.get(i) < min) {
                    min = laps.get(i);
                    minIndex = i;
                }
            }

            if (laps.size() > 1) {
                sb.append("━━━━━━━━━━━━━━━━━\n");
                sb.append("🥇 Vasca più veloce: Vasca ")
                        .append(minIndex + 1).append(" - ")
                        .append(MainActivity.formatTime(min)).append("\n");
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━\n");
        sb.append("Inviato con SwimTimer 🏊");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        // Prova ad aprire direttamente WhatsApp
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // WhatsApp non installato, apre il chooser generico
            intent.setPackage(null);
            startActivity(Intent.createChooser(intent,
                    getString(R.string.share_via)));
        }
    }

    private void populate() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(session.getName());
        binding.tvName.setText(session.getName());
        binding.tvDate.setText(DateFormat.getDateTimeInstance()
                .format(new Date(session.getDate())));
        binding.tvTotal.setText(MainActivity.formatTime(session.getTotalTime()));

        List<Long> laps = session.getLaps();
        if (laps == null || laps.isEmpty()) {
            binding.tvNoLaps.setVisibility(View.VISIBLE);
            binding.rvLaps.setVisibility(View.GONE);
        } else {
            binding.tvNoLaps.setVisibility(View.GONE);
            binding.rvLaps.setVisibility(View.VISIBLE);
            binding.rvLaps.setLayoutManager(new LinearLayoutManager(this));
            binding.rvLaps.setAdapter(new DetailLapAdapter(laps));
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(i);
    }
}
