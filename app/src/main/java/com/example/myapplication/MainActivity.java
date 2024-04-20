package com.example.myapplication;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.provider.Settings;

import android.content.Intent;
import android.widget.Toast;

import com.example.myapplication.Service.AppUsageService;
import com.example.myapplication.Service.DeviceEventService;
import com.example.myapplication.Worker.AppUsageWorker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.myapplication.Adapter.UsageStatsAdapter;
import com.example.myapplication.Model.UsageStatsModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends Activity {
    private TextView uptimeTextView;
    private static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference databaseReference;
    private FirebaseFirestore FireStoreDB;

    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;
    private LocalDate today;
    private RecyclerView recyclerView;
    private UsageStatsAdapter adapter;
    private BroadcastReceiver updateUIReceiver;
    private ArrayList<UsageStatsModel> appUsageInfoList = new ArrayList<>();
    private Handler handler = new Handler();


    String deviceId; // Get the unique device ID
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            long uptimeMillis = SystemClock.elapsedRealtime();
            long hours = (uptimeMillis / (1000 * 60 * 60)) % 24;
            long minutes = (uptimeMillis / (1000 * 60)) % 60;
            long seconds = (uptimeMillis / 1000) % 60;

            uptimeTextView.setText("Device Uptime: " + hours + "h " + minutes + "m " + seconds + "s");
            updateUptime(hours, minutes, seconds);
//            handler.postDelayed(this, 5); // Schedule this runnable to run again after 1 second
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        // Set your content view
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();

        button = findViewById(R.id.logout);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText("Welcome " + user.getEmail());
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
//                auth.signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        // Check if usage stats permission is granted
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        } else {
            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

//            schedulePeriodicAppUsageCheck();
            Intent serviceIntent = new Intent(this, DeviceEventService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            fetchAppUsageStats();
        }
    }

    private void schedulePeriodicAppUsageCheck() {
        // Define constraints, if needed (e.g., device charging, network connectivity)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Example: Require network connectivity
                .build();

        // Create a PeriodicWorkRequest for AppUsageWorker to run every 15 minutes
        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(AppUsageWorker.class, 15, TimeUnit.MINUTES) // Minimum period is 15 minutes
                        .setConstraints(constraints)
                        .build();

        // Enqueue the work with a unique name and a policy to replace existing work with the same name
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                "appUsageStatsCheck", // Unique name for the work
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, // Replace existing work with this name if it exists
                periodicWorkRequest); // The work request
    }



    private boolean hasUsageStatsPermission() {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void requestUsageStatsPermission() {
        Toast.makeText(this, "Please grant usage access permission.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void startAppUsageService() {
        Intent serviceIntent = new Intent(this, AppUsageService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USAGE_STATS_PERMISSION_REQUEST_CODE) {
            // Check again if usage stats permission is granted after returning from settings screen
            if (hasUsageStatsPermission()) {
                // Permission granted, start the service
                startAppUsageService();
            } else {
                Toast.makeText(this, "Usage access permission not granted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void updateUptime(long hours, long minutes, long seconds) {
//        databaseReference.child("devices").child(deviceId).child("uptime").child("hours").setValue(hours);
//        databaseReference.child("devices").child(deviceId).child("uptime").child("minutes").setValue(minutes);
//        databaseReference.child("devices").child(deviceId).child("uptime").child("seconds").setValue(seconds);

        Map<String, Object> uptime = new HashMap<>();
//        uptime.put("Date", today.toString());
        uptime.put("hours", hours);
        uptime.put("minutes", minutes);
        uptime.put("seconds", seconds);
        FireStoreDB.collection("Devices").document(deviceId).collection(today.toString())
                .document(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .set(uptime);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void fetchAppUsageStats() {
//        List<UsageStatsModel> appUsageInfoList = new ArrayList<>();
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

        // Query the usage stats for the current day
        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        Map<String, UsageStatsModel> aggregatedUsageStats = new HashMap<>();

        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            for (UsageStats usageStats : usageStatsList) {
                String packageName = usageStats.getPackageName();
                long usageDuration = usageStats.getTotalTimeInForeground();

                if (usageDuration > 0) {
                    UsageStatsModel existingModel = aggregatedUsageStats.get(packageName);
                    if (existingModel != null) {
                        // Update existing usage duration
                        existingModel.setUsageDuration(existingModel.getUsageDuration() + usageDuration);
                    } else {
                        // Add new entry
                        try {
                            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                            String appName = (String) packageManager.getApplicationLabel(applicationInfo);
                            aggregatedUsageStats.put(packageName, new UsageStatsModel(appName, usageDuration));
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace(); // Handle error
                        }
                    }
                }
            }
        }

        // Assuming adapter and recyclerView are already defined and initialized
        List<UsageStatsModel> appUsageInfoList = new ArrayList<>(aggregatedUsageStats.values());
        appUsageInfoList.sort((o1, o2) -> Long.compare(o2.getUsageDuration(), o1.getUsageDuration()));

        adapter = new UsageStatsAdapter(appUsageInfoList);
        recyclerView.setAdapter(adapter);
    }



//    private boolean hasUsageStatsPermission() {
//        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
//        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
//                android.os.Process.myUid(), getPackageName());
//        return mode == AppOpsManager.MODE_ALLOWED;
//    }

    private void applyTextViewStyle(TextView textView) {
        textView.setTextSize(24); // SP
        textView.setTextColor(Color.parseColor("#000000")); // A shade of purple
        textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//        textView.setShadowLayer(1.5f, 5, 5, Color.parseColor("#80000000")); // Shadow


        int paddingDp = 16;
        float density = getResources().getDisplayMetrics().density;
        int paddingPixel = (int) (paddingDp * density);
        textView.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel);

        // Check and set background with rounded corners
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textView.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_background));
            textView.setClipToOutline(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimerThread); // Stop the timer when the activity is destroyed
    }


}
