package com.swimtimer.app;
import java.util.List;

public class SessionData {
    private String name, id;
    private long date, totalTime;
    private List<Long> laps;

    public SessionData(String name, long date, long totalTime, List<Long> laps) {
        this.name = name; this.date = date; this.totalTime = totalTime; this.laps = laps;
        this.id = String.valueOf(date);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getDate() { return date; }
    public long getTotalTime() { return totalTime; }
    public List<Long> getLaps() { return laps; }
    public void setName(String n) { name = n; }
}
