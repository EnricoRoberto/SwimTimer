package com.swimtimer.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DetailLapAdapter extends RecyclerView.Adapter<DetailLapAdapter.VH> {
    private final List<Long> laps;
    private int recordType = SessionStorage.RECORD_NONE;

    public DetailLapAdapter(List<Long> laps) { this.laps = laps; }

    public void setRecordType(int recordType) {
        this.recordType = recordType;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_detail_lap, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        long lapTime = laps.get(i);

        long fastest = Long.MAX_VALUE, slowest = 0;
        for (Long l : laps) {
            if (l < fastest) fastest = l;
            if (l > slowest) slowest = l;
        }

        h.tvLapNumber.setText("Vasca " + (i + 1));
        h.tvLapTime.setText(MainActivity.formatTime(lapTime));

        if (lapTime == fastest && laps.size() > 1) {
            h.tvLapTime.setTextColor(0xFF4CAF50);
            h.tvLapNumber.setTextColor(0xFF4CAF50);
        } else if (lapTime == slowest && laps.size() > 1) {
            h.tvLapTime.setTextColor(0xFFF44336);
            h.tvLapNumber.setTextColor(0xFFF44336);
        } else {
            h.tvLapTime.setTextColor(0xFFFFFFFF);
            h.tvLapNumber.setTextColor(0xFFFFFFFF);
        }

        // Icona record solo sulla vasca più veloce
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
