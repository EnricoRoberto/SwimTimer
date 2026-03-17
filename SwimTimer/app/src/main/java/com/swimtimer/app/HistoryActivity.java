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
                adapter = new SessionListAdapter(sessions,
                    session -> {
                        Intent intent = new Intent(this, SessionDetailActivity.class);
                        intent.putExtra("session_id", session.getId());
                        startActivity(intent);
                    },
                    (session, position) -> {
                        android.app.AlertDialog deleteDialog = new android.app.AlertDialog.Builder(this)
                            .setTitle("Elimina gara")
                            .setMessage("Eliminare \"" + session.getName() + "\"?")
                            .setPositiveButton("Elimina", (d, w) -> {
                                SessionStorage.deleteSession(this, session.getId());
                                sessions.remove(position);
                                adapter.updateSessions(sessions);
                                if (sessions.isEmpty()) {
                                    binding.tvEmpty.setVisibility(View.VISIBLE);
                                    binding.rvSessions.setVisibility(View.GONE);
                                }
                            })
                            .setNegativeButton("Annulla", null)
                            .create();

                    deleteDialog.show();

                    if (deleteDialog.getWindow() != null)
                        deleteDialog.getWindow().setBackgroundDrawable(
                                new android.graphics.drawable.ColorDrawable(
                                        android.graphics.Color.WHITE));

                    deleteDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(android.graphics.Color.parseColor("#F44336"));
                    deleteDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(android.graphics.Color.parseColor("#757575"));

                    android.widget.TextView msgView =
                            deleteDialog.findViewById(android.R.id.message);
                    if (msgView != null)
                        msgView.setTextColor(android.graphics.Color.parseColor("#212121"));

                    android.widget.TextView titleView =
                            deleteDialog.findViewById(androidx.appcompat.R.id.alertTitle);
                    if (titleView != null)
                        titleView.setTextColor(android.graphics.Color.parseColor("#0D3B5E"));
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
