package com.swimtimer.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.swimtimer.app.databinding.ActivityHistoryBinding;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding binding;

    @Override protected void onCreate(Bundle s) {
        ThemeManager.getInstance(this).applyTheme(this);
        super.onCreate(s);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.history);
        }
        binding.rvSessions.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void load() {
        List<SessionData> sessions = SessionStorage.getSessions(this);
        if (sessions.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvSessions.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvSessions.setVisibility(View.VISIBLE);
            binding.rvSessions.setAdapter(new SessionListAdapter(sessions,
                s -> { Intent i = new Intent(this, SessionDetailActivity.class);
                       i.putExtra("sid", s.getId()); startActivity(i); },
                s -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete_q)
                        .setMessage(getString(R.string.delete_confirm, s.getName()))
                        .setPositiveButton(R.string.delete, (d, w) -> { SessionStorage.deleteSession(this, s.getId()); load(); })
                        .setNegativeButton(R.string.cancel, null).show()
            ));
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(i);
    }
}
