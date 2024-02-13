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
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.provider.Settings;

import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private TextView uptimeTextView;
    private RecyclerView recyclerView;
    private UsageStatsAdapter adapter;
    private Handler handler = new Handler();
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            long uptimeMillis = SystemClock.elapsedRealtime();
            long hours = (uptimeMillis / (1000 * 60 * 60)) % 24;
            long minutes = (uptimeMillis / (1000 * 60)) % 60;
            long seconds = (uptimeMillis / 1000) % 60;

            uptimeTextView.setText("Device Uptime: " + hours + "h " + minutes + "m " + seconds + "s");
            updateUptime(getApplicationContext(), hours, minutes, seconds);
            handler.postDelayed(this, 1000); // Schedule this runnable to run again after 1 second
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

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

    public void updateUptime(Context context, long hours, long minutes, long seconds) {
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID); // Get the unique device ID
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("devices").child(deviceId).child("uptime").child("hours").setValue(hours);
        databaseReference.child("devices").child(deviceId).child("uptime").child("minutes").setValue(minutes);
        databaseReference.child("devices").child(deviceId).child("uptime").child("seconds").setValue(seconds);
    }


    // Fetch app usage stats
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
                    appUsageInfoList.add(new UsageStatsModel(appName, usageStats.getTotalTimeInForeground()));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace(); // Handle error
                }
            }
        }
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
