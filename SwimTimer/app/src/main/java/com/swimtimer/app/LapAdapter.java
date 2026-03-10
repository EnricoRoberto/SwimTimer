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
    public LapAdapter(List<Long> laps) { this.laps = laps; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_lap, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        long t = laps.get(pos);
        h.num.setText(h.itemView.getContext().getString(R.string.lap_n, laps.size() - pos));
        h.time.setText(MainActivity.formatTime(t));
        if (laps.size() > 1) {
            long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (long l : laps) { if (l < min) min = l; if (l > max) max = l; }
            if (t == min) h.time.setTextColor(0xFF4CAF50);
            else if (t == max) h.time.setTextColor(0xFFF44336);
            else h.time.setTextColor(0xFFFFFFFF);
        } else { h.time.setTextColor(0xFFFFFFFF); }
    }

    @Override public int getItemCount() { return laps.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView num, time;
        VH(@NonNull View v) { super(v); num = v.findViewById(R.id.tvLapNum); time = v.findViewById(R.id.tvLapTime); }
    }
}
