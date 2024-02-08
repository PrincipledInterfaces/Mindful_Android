package com.example.myapplication;

public class UsageStatsModel {
    String packageName;
    long usageDuration; // in milliseconds

    public UsageStatsModel(String packageName, long usageDuration) {
        this.packageName = packageName;
        this.usageDuration = usageDuration;
    }

    // Getters
    public String getPackageName() {
        return packageName;
    }

    public long getUsageDuration() {
        return usageDuration;
    }

}
