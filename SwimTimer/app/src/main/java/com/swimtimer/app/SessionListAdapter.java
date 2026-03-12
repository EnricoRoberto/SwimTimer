package com.swimtimer.app;

import android.graphics.BitmapFactory;
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
import java.util.List;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.VH> {
    private List<SessionData> sessions;
    private final OnSessionClickListener listener;
    private final OnSessionDeleteListener deleteListener;

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
    }

    public void updateSessions(List<SessionData> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
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

        h.itemView.setOnClickListener(v -> listener.onSessionClick(s));
        h.btnDelete.setOnClickListener(v -> deleteListener.onSessionDelete(s, h.getAdapterPosition()));
    }

    @Override public int getItemCount() { return sessions.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, date, time, thumbPlaceholder;
        ImageView thumb;
        ImageButton btnDelete;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tvSessionName);
            date = v.findViewById(R.id.tvSessionDate);
            time = v.findViewById(R.id.tvSessionTime);
            thumb = v.findViewById(R.id.ivThumb);
            thumbPlaceholder = v.findViewById(R.id.tvThumbPlaceholder);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
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
}
