package com.example.myapplication;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.myapplication.Adapter.UsageStatsAdapter;
import com.example.myapplication.Model.UsageStatsModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends Activity {
    private TextView uptimeTextView;
    private DatabaseReference databaseReference;
    private FirebaseFirestore FireStoreDB;
    private LocalDate today;
    private RecyclerView recyclerView;
    private UsageStatsAdapter adapter;
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
            handler.postDelayed(this, 300000); // Schedule this runnable to run again after 1 second
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        today = LocalDate.now();

        // Realtime DB
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // firestore DB
        FireStoreDB = FirebaseFirestore.getInstance();

        deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        setContentView(R.layout.activity_main);
        uptimeTextView = findViewById(R.id.uptimeDynamicTextView);

        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            Toast.makeText(this, "Please grant usage access permission.", Toast.LENGTH_LONG).show();
        } else {
            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            fetchAppUsageStats();
        }


        handler.post(updateTimerThread); // Start the timer to update the uptime
    }

    public void updateUptime(long hours, long minutes, long seconds) {
        databaseReference.child("devices").child(deviceId).child("uptime").child("hours").setValue(hours);
        databaseReference.child("devices").child(deviceId).child("uptime").child("minutes").setValue(minutes);
        databaseReference.child("devices").child(deviceId).child("uptime").child("seconds").setValue(seconds);

        Map<String, Object> uptime = new HashMap<>();
        uptime.put("Date", today.toString());
        uptime.put("hours", hours);
        uptime.put("minutes", minutes);
        uptime.put("seconds", seconds);
        FireStoreDB.collection("devices").document(deviceId)
                .set(uptime);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void fetchAppUsageStats() {
        List<UsageStatsModel> appUsageInfoList = new ArrayList<>();
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = getPackageManager();
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.YEAR, -1); // Adjust according to your needs
        long startTime = calendar.getTimeInMillis();

        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            for (UsageStats usageStats : usageStatsList) {
                String packageName = usageStats.getPackageName();
                try {
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                    String appName = (String) packageManager.getApplicationLabel(applicationInfo);
                    long usageDuration = usageStats.getTotalTimeInForeground();
                    appUsageInfoList.add(new UsageStatsModel(appName, usageDuration));

                    // Upload each app's usage stats to Firebase
                    Map<String, Object> appUsageUpdate = new HashMap<>();
                    appUsageUpdate.put("appName", appName);
                    appUsageUpdate.put("usageDuration(minutes)", TimeUnit.MILLISECONDS.toMinutes(usageDuration));

                    // Use the package name as a unique key for each app
                    if (packageName != null) {
                        String safePackageName = packageName.replaceAll("[.$\\[\\]#\\/]", "_");
                        databaseReference.child("devices").child(deviceId).child("appUsageStats").child(safePackageName).setValue(appUsageUpdate);
                    } else {
                        Log.e("FirebaseError", "Package name is null");
                    }

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace(); // Handle error
                }
            }
        }

        // Assuming adapter and recyclerView are already defined and initialized
        appUsageInfoList.sort((o1, o2) -> Long.compare(o2.getUsageDuration(), o1.getUsageDuration()));

        adapter = new UsageStatsAdapter(appUsageInfoList);
        recyclerView.setAdapter(adapter);
    }



    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void applyTextViewStyle(TextView textView) {
        textView.setTextSize(24); // SP
        textView.setTextColor(Color.parseColor("#000000")); // A shade of purple
        textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//        textView.setShadowLayer(1.5f, 5, 5, Color.parseColor("#80000000")); // Shadow

        // Additional styling as before...

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
