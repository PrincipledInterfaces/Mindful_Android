package com.example.myapplication.Worker;

import android.app.usage.UsageEvents;
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
    Context context;
    public DeviceEventWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
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
        String DeviceModel = Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        FirebaseFirestore firestoreDB = FirebaseFirestore.getInstance();
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = context.getPackageManager();

        UsageEvents events = usm.queryEvents(unlockTime, lockTime);
        Map<String, UsageStatsModel> aggregatedUsage = new HashMap<>();
        Map<String, Long> lastForegroundTime = new HashMap<>();

        while (events.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            events.getNextEvent(event);

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundTime.put(event.getPackageName(), event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                long timeBgd = event.getTimeStamp();
                Long foregroundTime = lastForegroundTime.remove(event.getPackageName());
                if (foregroundTime != null) {
                    long timeSpent = timeBgd - foregroundTime;
                    if (timeSpent > 0) {
                        String packageName = event.getPackageName();
                        String appName;
                        try {
                            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                            appName = (String) packageManager.getApplicationLabel(applicationInfo);
                        } catch (PackageManager.NameNotFoundException ex) {
                            appName = packageName; // Fallback to package name if the app name is not found
                            Log.e("AppUsageWorker", "Error fetching app info for " + packageName, ex);
                            continue; // Skip this package
                        }
                        UsageStatsModel appUsageInfo = aggregatedUsage.getOrDefault(packageName, new UsageStatsModel(appName, timeSpent));
//                        appUsageInfo.addUsageTime(timeSpent);
                        aggregatedUsage.put(packageName, appUsageInfo);
                    }
                }
            }
        }

        if (!aggregatedUsage.isEmpty()) {
            String deviceIdConcat = "/" + deviceId + "-" + DeviceModel + "/";
            String dateObj = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "/";
            // Format start and end times
            String startTimeObj = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(unlockTime));
            String endTimeObj = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(lockTime));
            String timeDurObj = startTimeObj + "-" + endTimeObj + "/";

            // Iterate through each entry in the aggregated usage map
            for (Map.Entry<String, UsageStatsModel> entry : aggregatedUsage.entrySet()) {
                if (!entry.getKey().equals("com.sec.android.app.launcher")) {
                    String packageName = entry.getKey();
                    UsageStatsModel appUsageInfo = entry.getValue();
                    long[] formattedTime = appUsageInfo.getFormattedUsageTime(); // Assumes this method exists and returns an array of [hours, minutes, seconds]

                    Map<String, Object> appUsage = new HashMap<>();
                    appUsage.put("app_name", appUsageInfo.getAppName());
                    appUsage.put("foreground_time", new HashMap<String, Object>() {{
                        put("hours", formattedTime[0]);
                        put("minutes", formattedTime[1]);
                        put("seconds", formattedTime[2]);
                    }});

                    String documentPath = deviceIdConcat + dateObj + timeDurObj + packageName;
                    firestoreDB.document(documentPath)
                            .set(appUsage)
                            .addOnSuccessListener(aVoid -> Log.d("AppUsageWorker", "Successfully stored app usage stats for " + packageName))
                            .addOnFailureListener(e -> Log.e("AppUsageWorker", "Error storing app usage stats for " + packageName, e));
                }
                }
        }

    }
}

