package com.example.myapplication.Worker;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.Model.UsageStatsModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceEventWorker extends Worker {

    String deviceId;
    public DeviceEventWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Receive unlock and lock timestamps
        long unlockTime = getInputData().getLong("unlockTime", 0);
        long lockTime = getInputData().getLong("lockTime", System.currentTimeMillis());

        // It's assumed that validation ensuring unlockTime comes before lockTime is handled before enqueuing this worker
        if (unlockTime > 0 && lockTime > unlockTime) {

            storeDeviceEvent(unlockTime, lockTime);
        }

        return Result.success();
    }

    private void storeDeviceEvent(long unlockTime, long lockTime) {
        long duration = lockTime - unlockTime;

        String DeviceModel = Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
        deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        FirebaseFirestore firestoreDB = FirebaseFirestore.getInstance();
        Context context = getApplicationContext();
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = context.getPackageManager();


        // Query the usage stats
        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, unlockTime, lockTime);
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

                    appUsageInfo.addUsageTime(totalTimeInForeground);

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
                // create time obj for start time and end time
                String startTimeObj = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(unlockTime));
                String endTimeObj = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(lockTime));
                String TimeDurObj = startTimeObj + "-" + endTimeObj + "/";

//                String documentPath = "AppUsageStats" + deviceIdConcat + dateObj + TimeDurObj + "Apps/" +packageName;
                String documentPath = deviceIdConcat + dateObj + TimeDurObj + packageName;
                firestoreDB.document(documentPath)
                        .set(appUsage)
                        .addOnSuccessListener(aVoid -> Log.d("AppUsageWorker", "Successfully stored app usage stats for " + packageName))
                        .addOnFailureListener(e -> Log.e("AppUsageWorker", "Error storing app usage stats for " + packageName, e));
            }

        }
    }
}

