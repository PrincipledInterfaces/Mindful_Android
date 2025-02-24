package com.example.myapplication.Worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.app.usage.UsageStatsManager;
import android.app.usage.UsageStats;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.example.myapplication.Model.UsageStatsModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppUsageWorker extends Worker {
    String deviceId;

    public AppUsageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        fetchAndStoreAppUsageStats();
        return Result.success();
    }

    private void fetchAndStoreAppUsageStats() {
        String DeviceModel = Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
        deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Context context = getApplicationContext();
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = context.getPackageManager();
        FirebaseFirestore firestoreDB = FirebaseFirestore.getInstance();

        // Prepare the time range for today
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis(); // Start of today
        long endTime = System.currentTimeMillis(); // Current time

        // Query the usage stats
        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        Map<String, UsageStatsModel> aggregatedUsage = new HashMap<>();

        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            for (UsageStats usageStats : usageStatsList) {
                long totalTimeInForeground = usageStats.getTotalTimeInForeground();
                String packageName = usageStats.getPackageName();

                if (totalTimeInForeground > 0) {
                    String appName;
                    try {
                        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                        appName = (String) packageManager.getApplicationLabel(applicationInfo);
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName; // Fallback to package name if the app name is not found
                        Log.e("AppUsageWorker", "Error fetching app info for " + packageName, e);
                        continue; // Skip this package
                    }

                    UsageStatsModel appUsageInfo = aggregatedUsage.getOrDefault(packageName, new UsageStatsModel(appName, 0));
                    if (appUsageInfo != null) {
                        appUsageInfo.addUsageTime(totalTimeInForeground);
                    }
                    aggregatedUsage.put(packageName, appUsageInfo);
                }
            }

            // Write aggregated data to Firestore
            for (Map.Entry<String, UsageStatsModel> entry : aggregatedUsage.entrySet()) {
                String packageName = entry.getKey();
                UsageStatsModel appUsageInfo = entry.getValue();
                long[] formattedTime = appUsageInfo.getFormattedUsageTime();

                Map<String, Object> appUsage = new HashMap<>();
                Map<String, Object> timeAcum = new HashMap<>();

                appUsage.put("app_name", appUsageInfo.getAppName());

                timeAcum.put("hours", formattedTime[0]);
                timeAcum.put("minutes", formattedTime[1]);
                timeAcum.put("seconds", formattedTime[2]);

                appUsage.put("foreground_time", timeAcum);

                String deviceIdConcat = "/" + deviceId + "-" + DeviceModel + "/";
                String dateObj = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "/";
                String documentPath = "AppUsageStats" + deviceIdConcat + dateObj + packageName;
                firestoreDB.document(documentPath)
                        .set(appUsage)
                        .addOnSuccessListener(aVoid -> Log.d("AppUsageWorker", "Successfully stored app usage stats for " + packageName))
                        .addOnFailureListener(e -> Log.e("AppUsageWorker", "Error storing app usage stats for " + packageName, e));
            }
        }

    }
}
