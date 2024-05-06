package com.example.myapplication.Model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DeviceEvent {
    private String EventType;
    private long Time;
    private long duration;  // Only include this if your JSON data has a duration field

    private boolean isExpanded = false;
    private int expandableUntilIndex = -1;

//    private UsageStatsModel AppUsage;

    // Constructor
    public DeviceEvent(String EventType, long Time, int duration) {
        this.EventType = EventType;
        this.Time = Time;
        this.duration = duration;
    }

    // Getters
    public String getEventType() {
        return EventType;
    }

    public long getTime() {
        return Time*1000;
    }

    public long getDuration() {
        return duration;
    }

    // Setters
    public void setEventType(String eventType) {
        this.EventType = eventType;
    }

    public void setTime(long time) {
        this.Time = time;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public int getExpandableUntilIndex() {
        return expandableUntilIndex;
    }

    public void setExpandableUntilIndex(int index) {
        expandableUntilIndex = index;
    }
}
