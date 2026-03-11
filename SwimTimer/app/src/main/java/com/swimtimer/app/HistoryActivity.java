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
    private List<SessionData> sessions;
    private SessionListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.history));
        }

        sessions = SessionStorage.loadAll(this);
        refreshList();
    }

    private void refreshList() {
        if (sessions.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvSessions.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvSessions.setVisibility(View.VISIBLE);
            if (adapter == null) {
                adapter = new SessionListAdapter(sessions, session -> {
                    Intent intent = new Intent(this, SessionDetailActivity.class);
                    intent.putExtra("session_id", session.getId());
                    startActivity(intent);
                });
                binding.rvSessions.setLayoutManager(new LinearLayoutManager(this));
                binding.rvSessions.setAdapter(adapter);
            } else {
                adapter.updateSessions(sessions);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessions = SessionStorage.loadAll(this);
        refreshList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
