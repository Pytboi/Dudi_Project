package com.example.dudi_project.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "runs_table")
public class Run {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private long timestamp;
    private float distance;
    private long durationMillis;
    private float averagePace;
    private String routePoints;
    private String speedPoints; // שדה חדש לשמירת המהירויות לאורך הריצה

    public Run(long timestamp, float distance, long durationMillis, float averagePace, String routePoints, String speedPoints) {
        this.timestamp = timestamp;
        this.distance = distance;
        this.durationMillis = durationMillis;
        this.averagePace = averagePace;
        this.routePoints = routePoints;
        this.speedPoints = speedPoints;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public long getTimestamp() { return timestamp; }
    public float getDistance() { return distance; }
    public long getDurationMillis() { return durationMillis; }
    public float getAveragePace() { return averagePace; }
    public String getRoutePoints() { return routePoints; }
    public String getSpeedPoints() { return speedPoints; }
}