package com.swimtimer.app;

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

        binding.btnRename.setOnClickListener(v -> {
            View dv = getLayoutInflater().inflate(R.layout.dialog_save_session, null);
            com.google.android.material.textfield.TextInputEditText et = dv.findViewById(R.id.etSessionName);
            et.setText(session.getName());
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.rename).setView(dv)
                    .setPositiveButton(R.string.save, (d, w) -> {
                        String n = et.getText() != null ? et.getText().toString().trim() : "";
                        if (!n.isEmpty()) { session.setName(n); SessionStorage.updateSession(this, session); populate(); }
                    }).setNegativeButton(R.string.cancel, null).show();
        });
    }

    private void populate() {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(session.getName());
        binding.tvName.setText(session.getName());
        binding.tvDate.setText(DateFormat.getDateTimeInstance().format(new java.util.Date(session.getDate())));
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
