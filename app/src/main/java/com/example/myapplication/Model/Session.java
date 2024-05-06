package com.example.myapplication.Model;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session {
    private String startTime;
    private String endTime = "Ongoing";
    private List<Map<String, Object>> events = new ArrayList<>();
    private List<UsageStatsModel> appsUsed = new ArrayList<>();

    // Constructor to initialize the session with a start time
    public Session(String startTime) {
        this.startTime = startTime;
    }

    // Method to add an event to the session
    public void addEvent(String eventType, String timestamp) {
        Map<String, Object> event = new HashMap<>();
        event.put("Type", eventType);
        event.put("Timestamp", timestamp);
        this.events.add(event);
    }

    // Method to add an app usage to the session
    public void addAppUsage(UsageStatsModel appUsage) {
        this.appsUsed.add(appUsage);
    }

    // Setters and getters
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

//    public String getDuration(){
//        long dur = Long.parseLong(this.endTime) - Long.parseLong(this.startTime);
//        return Long.toString(dur/1000);
//    }
    public long getDuration() {
        // This method calculates the duration in seconds between startTime and endTime
        long durationInSeconds = calculateDurationInSeconds(this.startTime, this.endTime);
        return durationInSeconds;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public List<Map<String, Object>> getEvents() {
        return events;
    }

    public void setEvents(List<Map<String, Object>> events) {
        this.events = events;
    }

    public List<UsageStatsModel> getAppsUsed() {
        return appsUsed;
    }

    public void setAppsUsed(List<UsageStatsModel> appsUsed) {
        this.appsUsed = appsUsed;
    }

    private static long calculateDurationInSeconds(String startTime, String endTime) {
        // Define a formatter that matches the time format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Parse the start and end times using the formatter
        LocalTime startLocalTime = LocalTime.parse(startTime, formatter);
        LocalTime endLocalTime = LocalTime.parse(endTime, formatter);

        // Calculate duration between start and end times
        Duration duration = Duration.between(startLocalTime, endLocalTime);

        // Return the duration in seconds
        return duration.getSeconds();
    }


}

