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
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;
import android.os.Build;

import android.view.Menu;
import android.view.MenuItem;

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
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.Adapter.DeviceEventAdapter;
import com.example.myapplication.Model.DeviceEvent;
import com.example.myapplication.Util.AuthenticationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.*;

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
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.myapplication.Adapter.UsageStatsAdapter;
import com.example.myapplication.Model.UsageStatsModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int CREATE_EXPERIMENT_REQUEST_CODE = 1;
    private TextView uptimeTextView;
    private static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference databaseReference;
    private FirebaseFirestore FireStoreDB;

    FirebaseAuth auth;
    FloatingActionButton button;
    TextView textView;
    FirebaseUser user;
    MaterialToolbar toolbar;
    TextView summaryTextView;
    TextView runningExperimentDetailsTextView;
    private LocalDate today;
    private RecyclerView recyclerView;
    private UsageStatsAdapter adapter;
    private BroadcastReceiver updateUIReceiver;
    private ArrayList<UsageStatsModel> appUsageInfoList = new ArrayList<>();
    private Handler handler = new Handler();
    private String deviceIdConcat;

    String jsonData;
    List<DeviceEvent> events;

    String deviceId; // Get the unique device ID


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        // Set your content view
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.new_experiment);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();

        initializeComponents();

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText("Welcome, " + user.getEmail());
            toolbar = findViewById(R.id.top_app_toolbar);
            toolbar.setTitle("Dashboard");
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            // Check if usage stats permission is granted
            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission();
            } else {
                Intent serviceIntent = new Intent(this, DeviceEventService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                updateSummary();
                updateRunningExperimentDetails();
                fetchAndDisplayEvents();

            }
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Starting new Experiment", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), CreateExperiment.class);
//                startActivity(intent);
                startActivityForResult(intent, CREATE_EXPERIMENT_REQUEST_CODE);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRunningExperimentDetails();
    }

    private void initializeComponents() {
        FireStoreDB = FirebaseFirestore.getInstance();
        deviceIdConcat = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID) + "-" + Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
        runningExperimentDetailsTextView = findViewById(R.id.running_experiment_details);
    }

    private void updateRunningExperimentDetails() {
        // Fetch the running experiment details from Firestore or any other source
        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments")
                .whereEqualTo("isRunning", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot runningExperiment = task.getResult().getDocuments().get(0);
                        String title = runningExperiment.getString("title");
                        String goal = runningExperiment.getString("goal");
                        String schedule = runningExperiment.getString("schedule");
                        String duration = runningExperiment.getString("duration");
                        Long createdAt = runningExperiment.getLong("createdAt");

                        // Calculate the current day of the experiment
                        LocalDate startDate = Instant.ofEpochSecond(createdAt).atZone(ZoneOffset.UTC).toLocalDate();
                        LocalDate today = LocalDate.now(ZoneOffset.UTC);
                        long daysElapsed = ChronoUnit.DAYS.between(startDate, today) + 1;

                        // Determine if today is an intervention or control day based on the schedule
                        boolean isInterventionDay = isInterventionDay(Objects.requireNonNull(schedule), daysElapsed);

                        String dayStatus = isInterventionDay ? "an <font color='#FF0000'><b>INTERVENTION DAY</b>, be sure to use your intervention.</font>" : "a <font color='#00FF00'><b>CONTROL DAY</b></font>";
                        String message = "You are on <b>Day " + daysElapsed + "</b> of your <b>" + title + "</b> experiment; Today is " + dayStatus;

                        runningExperimentDetailsTextView.setText(Html.fromHtml(message));
                    } else {
                        runningExperimentDetailsTextView.setText("No Experiment Running!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching running experiment", e);
                    runningExperimentDetailsTextView.setText("Failed to load running experiment details");
                });
    }

    private boolean isInterventionDay(String schedule, long day) {
        switch (schedule) {
            case "Daily":
                return day % 2 == 0;
            case "Every 2 Days":
                return day % 4 == 3 || day % 4 == 0;
            case "Weekly":
                return (day / 7) % 2 != 0;
            default:
                return false; // Default to control day if schedule is not recognized
        }
    }

    public void fetchAndDisplayEvents(){
        try {
            jsonData = readJsonFromFile("DeviceEvent.json");
            events = parseDeviceEvents(jsonData);
            displayEvents(events);
        } catch (Exception e) {
            // This catch block handles any other unexpected errors
            Log.e("Reading Json File", "An unexpected error occurred", e);
            // Optionally, display a user-friendly message using Toast or Snackbar
        }
    }


    public void updateSummary() {
        summaryTextView = findViewById(R.id.summaryTextView);
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String formattedDate = today.format(dateFormatter);

        // Set the text to include today's date and daily summary
        String dailySummaryText = getResources().getString(R.string.daily_summary); // assuming you have this string in strings.xml
        summaryTextView.setText(formattedDate + " - " + dailySummaryText);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_logout) {
            AuthenticationUtils.logoutUser(this);
            return true;
        }
        else if (id == R.id.action_refresh) {
            updateRunningExperimentDetails();
            fetchAndDisplayEvents();
            Toast.makeText(MainActivity.this, "Data refreshed", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        if (requestCode == CREATE_EXPERIMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            updateRunningExperimentDetails();
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String readJsonFromFile(String filename) {
        StringBuilder json = new StringBuilder();
        try {
//            InputStream inputStream = getAssets().open(filename); // If stored in assets folder
             InputStream inputStream = openFileInput(filename); // If stored in internal storage
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();
            return json.toString();
        } catch (IOException e) {
            Log.e("MainActivity", "Error reading JSON file", e);
            return null;
        }
    }

    private List<DeviceEvent> parseDeviceEvents(String jsonData){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String todayDate = dateFormat.format(new Date());

        try {
            JSONObject jsonObject1 = new JSONObject(jsonData);

            JSONArray todayEvents = jsonObject1.getJSONArray(todayDate);

            List<DeviceEvent> eventsList = new ArrayList<>();

            for (int i = 0; i < todayEvents.length(); i++) {
                JSONObject eventObj = todayEvents.getJSONObject(i);

                String eventType = eventObj.getString("EventType");

                List<DeviceEvent> event = new ArrayList<>();
                event.add(new DeviceEvent(eventType, eventObj.getLong("Time"), 0));

                if (eventObj.has("AppUsage")) {
                    JSONArray appUsageArray = eventObj.getJSONArray("AppUsage");
                    for (int j = 0; j < appUsageArray.length(); j++) {
                        JSONObject appUsageEvent = appUsageArray.getJSONObject(j);
                        List<DeviceEvent> appEvent = new ArrayList<>();
                        appEvent.add(new DeviceEvent(appUsageEvent.getString("EventType"), appUsageEvent.getLong("Time"), appUsageEvent.getInt("Order")));

                        eventsList.add(appEvent.get(0));  // Treat each app usage as an independent event

                    }
                    eventsList.add(event.get(0)); // Add the main event
                }
                else {
                    eventsList.add(event.get(0));
                }
            }

            return eventsList;
//            Log.e("saeel", String.valueOf(eventsList.get(3).getEventType()));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();

    }




    private void displayEvents(List<DeviceEvent> events) {
        DeviceEventAdapter adapter = new DeviceEventAdapter(this, events);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }








}
