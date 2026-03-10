package com.swimtimer.app;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.*;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.VH> {
    public interface Click { void on(SessionData s); }

    private final List<SessionData> list;
    private final Click click, del;

    public SessionListAdapter(List<SessionData> list, Click click, Click del) {
        this.list = list; this.click = click; this.del = del;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_session, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        SessionData s = list.get(pos);
        h.name.setText(s.getName());
        h.date.setText(DateFormat.getDateTimeInstance().format(new Date(s.getDate())));
        h.total.setText(MainActivity.formatTime(s.getTotalTime()));
        int lc = s.getLaps() != null ? s.getLaps().size() : 0;
        h.laps.setText(h.itemView.getContext().getString(R.string.lap_count, lc));
        h.itemView.setOnClickListener(v -> click.on(s));
        h.del.setOnClickListener(v -> del.on(s));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, date, total, laps; ImageButton del;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tvSessionName); date = v.findViewById(R.id.tvSessionDate);
            total = v.findViewById(R.id.tvTotal); laps = v.findViewById(R.id.tvLapCount);
            del = v.findViewById(R.id.btnDel);
        }
    }
}
