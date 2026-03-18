package com.swimtimer.app;
import java.util.List;

public class SessionData {
    private String name, id, photoPath;
    private long date, totalTime;
    private List<Long> laps;
    private boolean imported;

    public SessionData(String name, long date, long totalTime, List<Long> laps) {
        this.name = name; this.date = date;
        this.totalTime = totalTime; this.laps = laps;
        this.id = String.valueOf(date);
        this.imported = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getDate() { return date; }
    public long getTotalTime() { return totalTime; }
    public List<Long> getLaps() { return laps; }
    public String getPhotoPath() { return photoPath; }
    public boolean isImported() { return imported; }
    public void setName(String n) { name = n; }
    public void setPhotoPath(String p) { photoPath = p; }
    public void setImported(boolean i) { imported = i; }
}
