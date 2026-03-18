package com.swimtimer.app;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.VH> {
    private List<SessionData> sessions;
    private final OnSessionClickListener listener;
    private final OnSessionDeleteListener deleteListener;
    private final Map<String, String> recordMap = new HashMap<>();

    public interface OnSessionClickListener {
        void onSessionClick(SessionData s);
    }

    public interface OnSessionDeleteListener {
        void onSessionDelete(SessionData s, int position);
    }

    public SessionListAdapter(List<SessionData> sessions,
                               OnSessionClickListener l,
                               OnSessionDeleteListener dl) {
        this.sessions = sessions;
        this.listener = l;
        this.deleteListener = dl;
        buildRecordMap();
    }

    public void updateSessions(List<SessionData> newSessions) {
        this.sessions = newSessions;
        buildRecordMap();
        notifyDataSetChanged();
    }

    private void buildRecordMap() {
        recordMap.clear();
        Map<String, Long> bestTimes = new HashMap<>();
        for (SessionData s : sessions) {
            String specialty = SessionStorage.extractSpecialty(s.getName());
            if (specialty.isEmpty() || s.getTotalTime() <= 0) continue;
            Long current = bestTimes.get(specialty);
            if (current == null || s.getTotalTime() < current) {
                bestTimes.put(specialty, s.getTotalTime());
                recordMap.put(specialty, s.getId());
            }
        }
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
            h.thumb.setImageBitmap(loadCorrectlyOrientedBitmap(photoPath));
        } else {
            h.thumb.setVisibility(View.GONE);
            h.thumbPlaceholder.setVisibility(View.VISIBLE);
        }

        // Badge importata
        if (s.isImported()) {
            h.tvImportedBadge.setVisibility(View.VISIBLE);
            h.tvImportedBadge.setBackgroundColor(Color.parseColor("#FFD600"));
            h.tvImportedBadge.setTextColor(Color.parseColor("#212121"));
        } else {
            h.tvImportedBadge.setVisibility(View.GONE);
        }

        // Badge record
        String specialty = SessionStorage.extractSpecialty(s.getName());
        String bestId = recordMap.get(specialty);
        boolean isRecord = bestId != null && bestId.equals(s.getId());
        if (isRecord) {
            h.cardBorder.setVisibility(View.VISIBLE);
            h.tvRecordBadge.setVisibility(View.VISIBLE);
        } else {
            h.cardBorder.setVisibility(View.GONE);
            h.tvRecordBadge.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onSessionClick(s));
        h.btnDelete.setOnClickListener(v ->
                deleteListener.onSessionDelete(s, h.getAdapterPosition()));
    }

    @Override public int getItemCount() { return sessions.size(); }

    public static android.graphics.Bitmap loadCorrectlyOrientedBitmap(String path) {
        try {
            android.graphics.Bitmap bmp = BitmapFactory.decodeFile(path);
            androidx.exifinterface.media.ExifInterface exif =
                    new androidx.exifinterface.media.ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);
            int degrees = 0;
            switch (orientation) {
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90; break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180; break;
                case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270; break;
            }
            if (degrees != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(degrees);
                bmp = android.graphics.Bitmap.createBitmap(
                        bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }
            return bmp;
        } catch (Exception e) {
            return BitmapFactory.decodeFile(path);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, date, time, thumbPlaceholder, tvRecordBadge, tvImportedBadge;
        ImageView thumb;
        ImageButton btnDelete;
        View cardBorder;

        VH(View v) {
            super(v);
            name             = v.findViewById(R.id.tvSessionName);
            date             = v.findViewById(R.id.tvSessionDate);
            time             = v.findViewById(R.id.tvSessionTime);
            thumb            = v.findViewById(R.id.ivThumb);
            thumbPlaceholder = v.findViewById(R.id.tvThumbPlaceholder);
            btnDelete        = v.findViewById(R.id.btnDelete);
            cardBorder       = v.findViewById(R.id.cardBorder);
            tvRecordBadge    = v.findViewById(R.id.tvRecordBadge);
            tvImportedBadge  = v.findViewById(R.id.tvImportedBadge);
        }
    }
}
