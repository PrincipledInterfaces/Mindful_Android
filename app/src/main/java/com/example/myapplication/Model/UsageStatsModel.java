package com.example.myapplication.Model;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class UsageStatsModel {
    private String appName; // Changed from packageName for clarity
    private long usageDuration;

    private int Order = 0;

    public UsageStatsModel(String appName, long usageDuration, int Order) {
        this.appName = appName;
        this.usageDuration = usageDuration;
        this.Order = Order;
    }

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

//    public void setOrder(int Order){this.Order = Order;}

    public int getOrder(){return Order;}

    public long getUsageDuration() {
        return usageDuration / 1000;
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

