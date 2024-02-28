package com.example.myapplication.Service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.Adapter.UsageStatsAdapter;
import com.example.myapplication.Model.UsageStatsModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppUsageService extends Service {

    private FirebaseFirestore firestoreDB;

    @Override
    public void onCreate() {
        super.onCreate();
        firestoreDB = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding, so return null
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchAndStoreAppUsageStats();
        return START_NOT_STICKY;
    }

    interface DataFetchListener {
        void onDataFetched(List<UsageStatsModel> fetchedData);
    }

//    private void fetchAndStoreAppUsageStats() {
//
//        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
//        Calendar calendar = Calendar.getInstance();
//        long endTime = calendar.getTimeInMillis();
//        calendar.add(Calendar.YEAR, -1); // Adjust based on your need
//        long startTime = calendar.getTimeInMillis();
//
//        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
//        if (usageStatsList != null && !usageStatsList.isEmpty()) {
//            for (UsageStats usageStats : usageStatsList) {
//                String packageName = usageStats.getPackageName();
//
//                long totalTimeInForeground = usageStats.getTotalTimeInForeground();
//
//                LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
//                LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//                Map<String, Object> appUsage = new HashMap<>();
//                appUsage.put("packageName", packageName);
//                appUsage.put("totalTimeInForeground", totalTimeInForeground);
//                appUsage.put("startTime", formatter.format(startDateTime));
//                appUsage.put("endTime", formatter.format(endDateTime));
//
//                // Use a more specific document path to organize data by date and time
//                String documentPath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "/" + "Apps" + "/" + packageName;
//                firestoreDB.document("AppUsageStats/" + documentPath)
//                        .set(appUsage)
//                        .addOnSuccessListener(aVoid -> {
//                            // Handle success
//                        })
//                        .addOnFailureListener(e -> {
//                            // Handle failure
//                        });
//
//            }
//        }
//    }

    private void fetchAndStoreAppUsageStats() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = getPackageManager();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); // Reset hour to the start of the day
        calendar.set(Calendar.MINUTE, 0);      // Reset minute
        calendar.set(Calendar.SECOND, 0);      // Reset second
        calendar.set(Calendar.MILLISECOND, 0); // Reset millisecond
        long startTime = calendar.getTimeInMillis(); // Start of the day

        // Keep the end time as the current time
        long endTime = System.currentTimeMillis(); // Current time

        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        Map<String, UsageStatsModel> aggregatedUsage = new HashMap<>();

        // Aggregate total time in foreground for each package, if greater than 0
        for (UsageStats usageStats : usageStatsList) {
            long totalTimeInForeground = usageStats.getTotalTimeInForeground();;

            String packageName = usageStats.getPackageName();


            if (totalTimeInForeground > 0) { // Check if the app has been used
                String appName = null;
                try {
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                    appName = (String) packageManager.getApplicationLabel(applicationInfo);
                } catch (PackageManager.NameNotFoundException e) {
                    appName = packageName; // Fallback to package name if app name not found
                    Log.e("AppUsageService", "Error fetching app info for " + packageName, e);
                }

                UsageStatsModel appUsageInfo = aggregatedUsage.getOrDefault(packageName, new UsageStatsModel(appName, 0));
                appUsageInfo.addUsageTime(totalTimeInForeground);
                aggregatedUsage.put(packageName, appUsageInfo);
            }
        }

        // Write aggregated data to Firestore, only for apps with foreground time
        for (Map.Entry<String, UsageStatsModel> entry : aggregatedUsage.entrySet()) {

            String packageName = entry.getKey();
            UsageStatsModel appUsageInfo = entry.getValue();

            if (appUsageInfo.getUsageDuration() > 0) { // Ensure there's meaningful data to store

                Map<String, Object> appUsage = new HashMap<>();
                appUsage.put("App name", appUsageInfo.getAppName());
//                appUsage.put("Usage Time", appUsageInfo.getFormattedUsageTime());
                appUsage.put("hours", appUsageInfo.getFormattedUsageTime()[0]);
                appUsage.put("minutes", appUsageInfo.getFormattedUsageTime()[1]);
                appUsage.put("seconds", appUsageInfo.getFormattedUsageTime()[2]);

                String documentPath = "AppUsageStats/" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "/Apps/" + packageName;
                firestoreDB.document(documentPath)
                        .set(appUsage)
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Successfully stored app usage stats for " + packageName))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error storing app usage stats for " + packageName, e));
            }
        }
    }

}

