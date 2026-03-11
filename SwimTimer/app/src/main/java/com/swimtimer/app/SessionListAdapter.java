package com.swimtimer.app;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.VH> {
    private final List<SessionData> sessions;
    private final OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(SessionData s);
    }

    public SessionListAdapter(List<SessionData> sessions, OnSessionClickListener l) {
        this.sessions = sessions; this.listener = l;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_session, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        SessionData s = sessions.get(i);
        h.name.setText(s.getName());
        h.date.setText(DateFormat.getDateTimeInstance()
                .format(new Date(s.getDate())));
        h.time.setText(MainActivity.formatTime(s.getTotalTime()));

        // Miniatura foto
        String photoPath = s.getPhotoPath();
        if (photoPath != null && !photoPath.isEmpty() && new File(photoPath).exists()) {
            h.thumb.setVisibility(View.VISIBLE);
            h.thumbPlaceholder.setVisibility(View.GONE);
            h.thumb.setImageBitmap(BitmapFactory.decodeFile(photoPath));
        } else {
            h.thumb.setVisibility(View.GONE);
            h.thumbPlaceholder.setVisibility(View.VISIBLE);
        }

        h.itemView.setOnClickListener(v -> listener.onSessionClick(s));
    }

    @Override public int getItemCount() { return sessions.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, date, time, thumbPlaceholder;
        ImageView thumb;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tvSessionName);
            date = v.findViewById(R.id.tvSessionDate);
            time = v.findViewById(R.id.tvSessionTime);
            thumb = v.findViewById(R.id.ivThumb);
            thumbPlaceholder = v.findViewById(R.id.tvThumbPlaceholder);
        }
    }
}
