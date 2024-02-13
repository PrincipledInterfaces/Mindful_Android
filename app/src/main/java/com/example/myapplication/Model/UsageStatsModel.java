package com.example.myapplication.Model;

public class UsageStatsModel {
    private String appName; // Changed from packageName for clarity
    private long usageDuration;

    public UsageStatsModel(String appName, long usageDuration) {
        this.appName = appName;
        this.usageDuration = usageDuration;
    }

    public String getAppName() {
        return appName;
    }

    public long getUsageDuration() {
        return usageDuration;
    }
}

