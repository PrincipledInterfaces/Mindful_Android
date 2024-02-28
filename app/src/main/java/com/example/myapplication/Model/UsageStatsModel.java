package com.example.myapplication.Model;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class UsageStatsModel {
    private String appName; // Changed from packageName for clarity
    private long usageDuration;

    public UsageStatsModel(String appName, long usageDuration) {
        this.appName = appName;
        this.usageDuration = usageDuration;
    }

    public void addUsageTime(long time) {
        this.usageDuration += time;
    }
    public String getAppName() {
        return appName;
    }

    public long getUsageDuration() {
        return usageDuration;
    }
    public void setUsageDuration(long usageDuration) {
        this.usageDuration = usageDuration;
    }

    public long[] getFormattedUsageTime() {
        long hours = TimeUnit.MILLISECONDS.toHours(usageDuration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(usageDuration) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(usageDuration) % 60;
//        return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);\
        return new long[]{hours, minutes, seconds};
    }

}

