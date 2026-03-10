package com.swimtimer.app;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DetailLapAdapter extends RecyclerView.Adapter<DetailLapAdapter.VH> {
    private final List<Long> laps;
    public DetailLapAdapter(List<Long> laps) { this.laps = laps; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_detail_lap, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        long t = laps.get(pos);
        long cum = 0; for (int i = 0; i <= pos; i++) cum += laps.get(i);
        h.num.setText(h.itemView.getContext().getString(R.string.lap_n, pos + 1));
        h.time.setText(MainActivity.formatTime(t));
        h.cum.setText("Σ " + MainActivity.formatTime(cum));
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (long l : laps) { if (l < min) min = l; if (l > max) max = l; }
        if (laps.size() > 1) {
            if (t == min) h.time.setTextColor(0xFF4CAF50);
            else if (t == max) h.time.setTextColor(0xFFF44336);
            else h.time.setTextColor(0xFF212121);
        }
    }

    @Override public int getItemCount() { return laps.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView num, time, cum;
        VH(@NonNull View v) { super(v);
            num = v.findViewById(R.id.tvDNum); time = v.findViewById(R.id.tvDTime); cum = v.findViewById(R.id.tvDCum); }
    }
}
