//package com.example.myapplication;
//
//import android.app.Activity;
//import android.app.AppOpsManager;
//import android.app.usage.UsageStats;
//import android.app.usage.UsageStatsManager;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.IntentFilter;
//import android.content.pm.ApplicationInfo;
//import android.content.pm.PackageManager;
//import android.graphics.Typeface;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.SystemClock;
//import android.provider.Settings;
//import android.text.Html;
//import android.util.Log;
//import android.view.View;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.Spinner;
//import android.widget.TextView;
//import android.widget.LinearLayout;
//import android.view.Gravity;
//import android.graphics.Color;
//import android.os.Build;
//
//import android.view.Menu;
//import android.view.MenuItem;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.RequiresApi;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.content.ContextCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import android.content.Context;
//import android.provider.Settings;
//
//import android.content.Intent;
//import android.widget.Toast;
//import androidx.appcompat.widget.Toolbar;
//
//import com.example.myapplication.Adapter.DeviceEventAdapter;
//import com.example.myapplication.Model.DeviceEvent;
//import com.example.myapplication.Util.AuthenticationUtils;
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.floatingactionbutton.*;
//
//import com.example.myapplication.Service.AppUsageService;
//import com.example.myapplication.Service.DeviceEventService;
//import com.example.myapplication.Worker.AppUsageWorker;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.firestore.DocumentSnapshot;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.lang.reflect.Type;
//import java.text.SimpleDateFormat;
//import java.time.Instant;
//import java.time.ZoneId;
//import java.time.ZoneOffset;
//import java.time.ZonedDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Objects;
//import java.util.TimeZone;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//import com.example.myapplication.Adapter.UsageStatsAdapter;
//import com.example.myapplication.Model.UsageStatsModel;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.gson.Gson;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParseException;
//import com.google.gson.reflect.TypeToken;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class MainActivity extends Activity {
//    private static final int CREATE_EXPERIMENT_REQUEST_CODE = 1;
//    private TextView uptimeTextView;
//    private static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 1;
//    private DatabaseReference databaseReference;
//    private FirebaseFirestore FireStoreDB;
//
//    FirebaseAuth auth;
//    FloatingActionButton button;
//    TextView textView;
//    FirebaseUser user;
//    MaterialToolbar toolbar;
//    TextView summaryTextView;
//    TextView runningExperimentDetailsTextView;
//    private LocalDate today;
//    private RecyclerView recyclerView;
//    private UsageStatsAdapter adapter;
//    private BroadcastReceiver updateUIReceiver;
//    private ArrayList<UsageStatsModel> appUsageInfoList = new ArrayList<>();
//    private Handler handler = new Handler();
//    private String deviceIdConcat;
//
//    String jsonData;
//    List<DeviceEvent> events;
//
//    String deviceId; // Get the unique device ID
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        FirebaseApp.initializeApp(this);
//        // Set your content view
//        setContentView(R.layout.activity_main);
//
//        auth = FirebaseAuth.getInstance();
//        button = findViewById(R.id.new_experiment);
//        textView = findViewById(R.id.user_details);
//        user = auth.getCurrentUser();
//
//        initializeComponents();
//
//        if (user == null) {
//            Intent intent = new Intent(getApplicationContext(), Login.class);
//            startActivity(intent);
//            finish();
//        } else {
//            textView.setText("Welcome, " + user.getEmail());
//            toolbar = findViewById(R.id.top_app_toolbar);
//            toolbar.setTitle("Dashboard");
//            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//                    return onOptionsItemSelected(item);
//                }
//            });
//            // Check if usage stats permission is granted
//            if (!hasUsageStatsPermission()) {
//                requestUsageStatsPermission();
//            } else {
//                Intent serviceIntent = new Intent(this, DeviceEventService.class);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    startForegroundService(serviceIntent);
//                } else {
//                    startService(serviceIntent);
//                }
//
//                updateSummary();
//                updateRunningExperimentDetails();
//                fetchAndDisplayEvents();
//
//            }
//        }
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "Starting new Experiment", Toast.LENGTH_LONG).show();
//                Intent intent = new Intent(getApplicationContext(), CreateExperiment.class);
////                startActivity(intent);
//                startActivityForResult(intent, CREATE_EXPERIMENT_REQUEST_CODE);
//            }
//        });
//
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        updateRunningExperimentDetails();
//    }
//
//    private void initializeComponents() {
//        FireStoreDB = FirebaseFirestore.getInstance();
//        deviceIdConcat = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID) + "-" + Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
//        runningExperimentDetailsTextView = findViewById(R.id.running_experiment_details);
//    }
//
//    private void updateRunningExperimentDetails() {
//        // Fetch the running experiment details from Firestore or any other source
//        FireStoreDB.collection("Devices").document(deviceIdConcat)
//                .collection("experiments")
//                .whereEqualTo("isRunning", true)
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
//                        DocumentSnapshot runningExperiment = task.getResult().getDocuments().get(0);
//                        String title = runningExperiment.getString("title");
//                        String goal = runningExperiment.getString("goal");
//                        String schedule = runningExperiment.getString("schedule");
//                        String duration = runningExperiment.getString("duration");
//                        Long createdAt = runningExperiment.getLong("createdAt");
//
//                        // Calculate the current day of the experiment
////                        LocalDate startDate = Instant.ofEpochSecond(createdAt).atZone(ZoneOffset.UTC).toLocalDate();
////                        LocalDate today = LocalDate.now(ZoneOffset.UTC);
////                        long daysElapsed = ChronoUnit.DAYS.between(startDate, today) + 1;
//                        if (createdAt != null) {
//                            Instant createdAtInstant = Instant.ofEpochSecond(createdAt);
//                            LocalDate startDate = createdAtInstant.atZone(ZoneId.systemDefault()).toLocalDate();
//                            LocalDate today = LocalDate.now(ZoneId.systemDefault());
//                            long daysElapsed = ChronoUnit.DAYS.between(startDate, today) + 1;
//
//                            // Determine if today is an intervention or control day based on the schedule
//                            boolean isInterventionDay = isInterventionDay(Objects.requireNonNull(schedule), daysElapsed);
//
//                            String dayStatus = isInterventionDay ? "an <font color='#FF0000'><b>INTERVENTION DAY</b></font>, be sure to use your intervention." : "a <font color='#00FF00'><b>CONTROL DAY</b></font>";
//                            String message = "You are on <b>Day " + daysElapsed + "</b> of your <b>" + title + "</b> experiment; Today is " + dayStatus;
//
//                            runningExperimentDetailsTextView.setText(Html.fromHtml(message));
//                        }
//                        else {
//                            runningExperimentDetailsTextView.setText("No Experiment Running!");
//                        }
//                    } else {
//                        runningExperimentDetailsTextView.setText("No Experiment Running!");
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("Firestore", "Error fetching running experiment", e);
//                    runningExperimentDetailsTextView.setText("Failed to load running experiment details");
//                });
//    }
//
//    private boolean isInterventionDay(String schedule, long day) {
//        switch (schedule) {
//            case "Daily":
//                return day % 2 == 0;
//            case "Every 2 Days":
//                return day % 4 == 3 || day % 4 == 0;
//            case "Weekly":
//                return (day / 7) % 2 != 0;
//            default:
//                return false; // Default to control day if schedule is not recognized
//        }
//    }
//
//    public void fetchAndDisplayEvents(){
//        try {
//            jsonData = readJsonFromFile("DeviceEvent.json");
//            events = parseDeviceEvents(jsonData);
//            displayEvents(events);
//        } catch (Exception e) {
//            // This catch block handles any other unexpected errors
//            Log.e("Reading Json File", "An unexpected error occurred", e);
//            // Optionally, display a user-friendly message using Toast or Snackbar
//        }
//    }
//
//
//    public void updateSummary() {
//        summaryTextView = findViewById(R.id.summaryTextView);
//        LocalDate today = LocalDate.now();
//        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
//        String formattedDate = today.format(dateFormatter);
//
//        // Set the text to include today's date and daily summary
//        String dailySummaryText = getResources().getString(R.string.daily_summary); // assuming you have this string in strings.xml
//        summaryTextView.setText(formattedDate + " - " + dailySummaryText);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//
//        if (id == R.id.menu_logout) {
//            AuthenticationUtils.logoutUser(this);
//            return true;
//        }
//        else if (id == R.id.action_refresh) {
//            updateRunningExperimentDetails();
//            fetchAndDisplayEvents();
//            Toast.makeText(MainActivity.this, "Data refreshed", Toast.LENGTH_SHORT).show();
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//
//    private boolean hasUsageStatsPermission() {
//        try {
//            PackageManager packageManager = getPackageManager();
//            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
//            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
//            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
//            return (mode == AppOpsManager.MODE_ALLOWED);
//        } catch (PackageManager.NameNotFoundException e) {
//            return false;
//        }
//    }
//
//    private void requestUsageStatsPermission() {
//        Toast.makeText(this, "Please grant usage access permission.", Toast.LENGTH_LONG).show();
//        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
//    }
//
//    private void startAppUsageService() {
//        Intent serviceIntent = new Intent(this, AppUsageService.class);
//        startService(serviceIntent);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == USAGE_STATS_PERMISSION_REQUEST_CODE) {
//            // Check again if usage stats permission is granted after returning from settings screen
//            if (hasUsageStatsPermission()) {
//                // Permission granted, start the service
//                startAppUsageService();
//            } else {
//                Toast.makeText(this, "Usage access permission not granted.", Toast.LENGTH_SHORT).show();
//            }
//        }
//        if (requestCode == CREATE_EXPERIMENT_REQUEST_CODE && resultCode == RESULT_OK) {
//            updateRunningExperimentDetails();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//    }
//
//    private String readJsonFromFile(String filename) {
//        StringBuilder json = new StringBuilder();
//        try {
////            InputStream inputStream = getAssets().open(filename); // If stored in assets folder
//             InputStream inputStream = openFileInput(filename); // If stored in internal storage
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                json.append(line);
//            }
//            reader.close();
//            return json.toString();
//        } catch (IOException e) {
//            Log.e("MainActivity", "Error reading JSON file", e);
//            return null;
//        }
//    }
//
//    private List<DeviceEvent> parseDeviceEvents(String jsonData){
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//        String todayDate = dateFormat.format(new Date());
//
//        try {
//            JSONObject jsonObject1 = new JSONObject(jsonData);
//
//            JSONArray todayEvents = jsonObject1.getJSONArray(todayDate);
//
//            List<DeviceEvent> eventsList = new ArrayList<>();
//
//            for (int i = 0; i < todayEvents.length(); i++) {
//                JSONObject eventObj = todayEvents.getJSONObject(i);
//
//                String eventType = eventObj.getString("EventType");
//
//                List<DeviceEvent> event = new ArrayList<>();
//                event.add(new DeviceEvent(eventType, eventObj.getLong("Time"), 0));
//
//                if (eventObj.has("AppUsage")) {
//                    JSONArray appUsageArray = eventObj.getJSONArray("AppUsage");
//                    for (int j = 0; j < appUsageArray.length(); j++) {
//                        JSONObject appUsageEvent = appUsageArray.getJSONObject(j);
//                        List<DeviceEvent> appEvent = new ArrayList<>();
//                        appEvent.add(new DeviceEvent(appUsageEvent.getString("EventType"), appUsageEvent.getLong("Time"), appUsageEvent.getInt("Order")));
//
//                        eventsList.add(appEvent.get(0));  // Treat each app usage as an independent event
//
//                    }
//                    eventsList.add(event.get(0)); // Add the main event
//                }
//                else {
//                    eventsList.add(event.get(0));
//                }
//            }
//
//            return eventsList;
////            Log.e("saeel", String.valueOf(eventsList.get(3).getEventType()));
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return new ArrayList<>();
//
//    }
//
//
//
//
//    private void displayEvents(List<DeviceEvent> events) {
//        DeviceEventAdapter adapter = new DeviceEventAdapter(this, events);
//        RecyclerView recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(adapter);
//    }
//
//
//
//
//
//
//
//
//}


package com.example.myapplication;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Adapter.DeviceEventAdapter;
import com.example.myapplication.Model.DeviceEvent;
import com.example.myapplication.Model.UsageStatsModel;
import com.example.myapplication.Service.AppUsageService;
import com.example.myapplication.Service.DeviceEventService;
import com.example.myapplication.Util.AuthenticationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private static final int CREATE_EXPERIMENT_REQUEST_CODE = 1;
    private static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 1;

    private FirebaseAuth auth;
    private FirebaseFirestore FireStoreDB;
    private FirebaseUser user;
    private String deviceIdConcat;
    private List<DeviceEvent> events;
    private String jsonData;

    private TextView textView;
    private TextView summaryTextView;
    private TextView runningExperimentDetailsTextView;
    private RecyclerView recyclerView;
    private FloatingActionButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        initializeComponents();

        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
        } else {
            textView.setText("Welcome, " + user.getEmail());
            setupToolbar();
            checkUsageStatsPermission();

            button.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Starting new Experiment", Toast.LENGTH_LONG).show();
                startActivityForResult(new Intent(this, CreateExperiment.class), CREATE_EXPERIMENT_REQUEST_CODE);
            });
        }
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
        summaryTextView = findViewById(R.id.summaryTextView);
        textView = findViewById(R.id.user_details);
        button = findViewById(R.id.new_experiment);
        recyclerView = findViewById(R.id.recyclerView);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_toolbar);
        toolbar.setTitle("Dashboard");
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    private void checkUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        } else {
            startDeviceEventService();
            updateSummary();
            fetchAndDisplayEvents();
        }
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
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_STATS_PERMISSION_REQUEST_CODE);
    }

    private void startDeviceEventService() {
        Intent serviceIntent = new Intent(this, DeviceEventService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateSummary() {
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()));
        String dailySummaryText = getResources().getString(R.string.daily_summary);
        summaryTextView.setText(String.format("%s - %s", formattedDate, dailySummaryText));
    }

    private void updateRunningExperimentDetails() {
        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments")
                .whereEqualTo("isRunning", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot runningExperiment = task.getResult().getDocuments().get(0);
                        displayExperimentDetails(runningExperiment);
                    } else {
                        runningExperimentDetailsTextView.setText("No Experiment Running!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching running experiment", e);
                    runningExperimentDetailsTextView.setText("Failed to load running experiment details");
                });
    }

    private void displayExperimentDetails(DocumentSnapshot experiment) {
        String title = experiment.getString("title");
        String goal = experiment.getString("goal");
        String schedule = experiment.getString("schedule");
        String duration = experiment.getString("duration");
        Long createdAt = experiment.getLong("createdAt");

        if (createdAt != null) {
            Instant createdAtInstant = Instant.ofEpochSecond(createdAt);
            LocalDate startDate = createdAtInstant.atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            long daysElapsed = ChronoUnit.DAYS.between(startDate, today) + 1;
            boolean isInterventionDay = isInterventionDay(Objects.requireNonNull(schedule), daysElapsed);

            String dayStatus = isInterventionDay ? "an <font color='#FF0000'><b>INTERVENTION DAY</b></font>, be sure to use your intervention." : "a <font color='#00FF00'><b>CONTROL DAY</b></font>";
            String message = "You are on <b>Day " + daysElapsed + "</b> of your <b>" + title + "</b> experiment; Today is " + dayStatus;

            runningExperimentDetailsTextView.setText(Html.fromHtml(message));
        } else {
            runningExperimentDetailsTextView.setText("No Experiment Running!");
        }
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
                return false;
        }
    }

    private void fetchAndDisplayEvents() {
        try {
            jsonData = readJsonFromFile("DeviceEvent.json");
            events = parseDeviceEvents(jsonData);
            displayEvents(events);
        } catch (Exception e) {
            Log.e("Reading Json File", "An unexpected error occurred", e);
        }
    }

    private String readJsonFromFile(String filename) {
        StringBuilder json = new StringBuilder();
        try (InputStream inputStream = openFileInput(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Error reading JSON file", e);
        }
        return json.toString();
    }

//    private List<DeviceEvent> parseDeviceEvents(String jsonData) {
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//        String todayDate = dateFormat.format(new Date());
//
//        try {
//            JSONObject jsonObject = new JSONObject(jsonData);
//            JSONArray todayEvents = jsonObject.getJSONArray(todayDate);
//            List<DeviceEvent> eventsList = new ArrayList<>();
//
//            for (int i = 0; i < todayEvents.length(); i++) {
//                JSONObject eventObj = todayEvents.getJSONObject(i);
//                String eventType = eventObj.getString("EventType");
//
//                DeviceEvent event = new DeviceEvent(eventType, eventObj.getLong("Time"), 0);
//                eventsList.add(event);
//
//                if (eventObj.has("AppUsage")) {
//                    JSONArray appUsageArray = eventObj.getJSONArray("AppUsage");
//                    for (int j = 0; j < appUsageArray.length(); j++) {
//                        JSONObject appUsageEvent = appUsageArray.getJSONObject(j);
//                        DeviceEvent appEvent = new DeviceEvent(appUsageEvent.getString("EventType"), appUsageEvent.getLong("Time"), appUsageEvent.getInt("Order"));
//                        eventsList.add(appEvent);
//                    }
//                }
//            }
//            return eventsList;
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return new ArrayList<>();
//    }
private List<DeviceEvent> parseDeviceEvents(String jsonData) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String todayDate = dateFormat.format(new Date());

    try {
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray todayEvents = jsonObject.getJSONArray(todayDate);
        List<DeviceEvent> eventsList = new ArrayList<>();

        String lastEventType = "";
        long lastEventTime = 0;

        for (int i = 0; i < todayEvents.length(); i++) {
            JSONObject eventObj = todayEvents.getJSONObject(i);
            String eventType = eventObj.getString("EventType");
            long eventTime = eventObj.getLong("Time");

            // Skip "Screen On" event if followed by "Device Unlocked" event within a short period (e.g., 5 seconds)
            if (eventType.equals("Device Unlocked") && lastEventType.equals("Screen On") && (eventTime - lastEventTime) < 5000) {
                // Remove the previously added "Screen On" event
                if (!eventsList.isEmpty()) {
                    eventsList.remove(eventsList.size() - 1);
                }
            }

            // Add the current event to the list
            DeviceEvent event = new DeviceEvent(eventType, eventTime, 0);
            eventsList.add(event);

            // Track the last event type and time
            lastEventType = eventType;
            lastEventTime = eventTime;

            if (eventObj.has("AppUsage")) {
                JSONArray appUsageArray = eventObj.getJSONArray("AppUsage");
                for (int j = 0; j < appUsageArray.length(); j++) {
                    JSONObject appUsageEvent = appUsageArray.getJSONObject(j);
                    DeviceEvent appEvent = new DeviceEvent(appUsageEvent.getString("EventType"), appUsageEvent.getLong("Time"), appUsageEvent.getInt("Order"));
                    eventsList.add(appEvent);
                }
            }
        }
        return eventsList;
    } catch (JSONException e) {
        e.printStackTrace();
    }
    return new ArrayList<>();
}


    private void displayEvents(List<DeviceEvent> events) {
        DeviceEventAdapter adapter = new DeviceEventAdapter(this, events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            AuthenticationUtils.logoutUser(this);
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            updateRunningExperimentDetails();
            fetchAndDisplayEvents();
            Toast.makeText(MainActivity.this, "Data refreshed", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USAGE_STATS_PERMISSION_REQUEST_CODE) {
            if (hasUsageStatsPermission()) {
                startAppUsageService();
            } else {
                Toast.makeText(this, "Usage access permission not granted.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CREATE_EXPERIMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            updateRunningExperimentDetails();
        }
    }

    private void startAppUsageService() {
        Intent serviceIntent = new Intent(this, AppUsageService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
