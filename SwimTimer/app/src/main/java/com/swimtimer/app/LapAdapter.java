package com.swimtimer.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LapAdapter extends RecyclerView.Adapter<LapAdapter.VH> {
    private final List<Long> laps;
    private int recordType = SessionStorage.RECORD_NONE;

    public LapAdapter(List<Long> laps) { this.laps = laps; }

    /** Chiamato da MainActivity dopo stop per mostrare l'icona record */
    public void setRecordType(int recordType) {
        this.recordType = recordType;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_lap, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        long lapTime = laps.get(i);

        // Trova fastest e slowest
        long fastest = Long.MAX_VALUE, slowest = 0;
        for (Long l : laps) {
            if (l < fastest) fastest = l;
            if (l > slowest) slowest = l;
        }

        int lapNumber = laps.size() - i;
        h.tvLapNumber.setText("Vasca " + lapNumber);
        h.tvLapTime.setText(MainActivity.formatTime(lapTime));

        // Colore base
        if (lapTime == fastest && laps.size() > 1) {
            h.tvLapTime.setTextColor(0xFF4CAF50); // verde
            h.tvLapNumber.setTextColor(0xFF4CAF50);
        } else if (lapTime == slowest && laps.size() > 1) {
            h.tvLapTime.setTextColor(0xFFF44336); // rosso
            h.tvLapNumber.setTextColor(0xFFF44336);
        } else {
            h.tvLapTime.setTextColor(0xFFFFFFFF);
            h.tvLapNumber.setTextColor(0xFFFFFFFF);
        }

        // Icona record — solo sulla vasca più veloce
        if (lapTime == fastest && laps.size() > 0) {
            switch (recordType) {
                case SessionStorage.RECORD_ABSOLUTE:
                    h.tvRecord.setText(" 🥇");
                    h.tvRecord.setVisibility(View.VISIBLE);
                    break;
                case SessionStorage.RECORD_IMPROVED:
                    h.tvRecord.setText(" 📈");
                    h.tvRecord.setVisibility(View.VISIBLE);
                    break;
                case SessionStorage.RECORD_BOTH:
                    h.tvRecord.setText(" 🥇📈");
                    h.tvRecord.setVisibility(View.VISIBLE);
                    break;
                default:
                    h.tvRecord.setVisibility(View.GONE);
            }
        } else {
            h.tvRecord.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return laps.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLapNumber, tvLapTime, tvRecord;
        VH(View v) {
            super(v);
            tvLapNumber = v.findViewById(R.id.tvLapNumber);
            tvLapTime   = v.findViewById(R.id.tvLapTime);
            tvRecord    = v.findViewById(R.id.tvRecord);
        }
    }
}
