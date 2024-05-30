//package com.example.myapplication;
//
//import android.content.SharedPreferences;
//import android.os.Build;
//import android.os.Bundle;
//import android.provider.Settings;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.ArrayAdapter;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.Toast;
//import android.app.DatePickerDialog;
//import java.text.SimpleDateFormat;
//import java.time.Clock;
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.text.ParseException;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//
//import com.example.myapplication.Util.AuthenticationUtils;
//import com.example.myapplication.Worker.NotificationWorker;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.switchmaterial.SwitchMaterial;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FieldPath;
//import com.google.firebase.firestore.FieldValue;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.Query;
//import com.google.firebase.messaging.FirebaseMessaging;
//
//import androidx.core.view.WindowInsetsCompat;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
//// notification worker
//import androidx.work.ExistingPeriodicWorkPolicy;
//import androidx.work.PeriodicWorkRequest;
//import androidx.work.WorkManager;
//import androidx.work.Data;
//
//import org.json.JSONObject;
//
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
//
//public class CreateExperiment extends AppCompatActivity {
//    private EditText experimentTitleInput;
//    private EditText experimentGoalInput;
//    private EditText stepsTakenInput;
//
//    private Spinner scheduleSpinner;
//    private SwitchMaterial runningSwitch;
//
//    private String deviceId;
//
//    String DeviceModel;
//    private View loadingScreen;
//    private FirebaseFirestore FireStoreDB;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_create_experiment);
//
//        FireStoreDB = FirebaseFirestore.getInstance();
//
//        MaterialToolbar toolbar = findViewById(R.id.top_app_toolbar);
//        setSupportActionBar(toolbar);
//        Objects.requireNonNull(getSupportActionBar()).setTitle("Create Experiment");
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                return onOptionsItemSelected(item);
//            }
//        });
//
//        if (deviceId == null) {
//            deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
//        }
//        if (DeviceModel ==  null){
//            DeviceModel = Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
//        }
//
//        experimentTitleInput = findViewById(R.id.experiment_title);
//        experimentGoalInput = findViewById(R.id.experiment_goal);
//        stepsTakenInput = findViewById(R.id.steps_taken);
//        scheduleSpinner = findViewById(R.id.schedule_spinner);
//        runningSwitch = findViewById(R.id.running_switch);
//        loadingScreen = findViewById(R.id.loading_screen);
//
//        fetchLastExperiment();
//
//        // Button initialization and setOnClickListener
//        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                submitExperiment();
//            }
//        });
//
////        FirebaseMessaging.getInstance().getToken()
////                .addOnCompleteListener(new OnCompleteListener<String>() {
////                    @Override
////                    public void onComplete(@NonNull Task<String> task) {
////                        if (!task.isSuccessful()) {
////                            Log.w("fcm", "Fetching FCM registration token failed", task.getException());
////                            return;
////                        }
////
////                        // Get new FCM registration token
////                        String token = task.getResult();
////
////                        // Log and toast
////
////                        Log.d("fcm", token);
////                        Toast.makeText(CreateExperiment.this, "fcm", Toast.LENGTH_SHORT).show();
////                    }
////                });
//
//    }
//
//    private void showLoadingScreen() {
//        loadingScreen.setVisibility(View.VISIBLE);
//    }
//
//    private void hideLoadingScreen() {
//        loadingScreen.setVisibility(View.GONE);
//    }
//
//    private void submitExperiment() {
//        showLoadingScreen();
//        // Retrieving the text from EditText fields
//        String title = experimentTitleInput.getText().toString();
//        String goal = experimentGoalInput.getText().toString();
//        String steps = stepsTakenInput.getText().toString();
//        String schedule = scheduleSpinner.getSelectedItem().toString();
//        boolean isRunning = runningSwitch.isChecked();
//
//        if(title.isEmpty() || goal.isEmpty() || steps.isEmpty()) {
//            Toast.makeText(CreateExperiment.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
//            hideLoadingScreen();
//        } else {
//            saveExperimentToFirestore(title, goal, steps, schedule, isRunning);
//            Toast.makeText(CreateExperiment.this, "Experiment Submitted Successfully", Toast.LENGTH_SHORT).show();
//
//        }
//    }
//
////    private void scheduleNotificationWorker(String title, String description, String schedule) {
////        Data data = new Data.Builder()
////                .putString("title", "Today is an INTERVENTION DAY for your <i>" + title + "</i> experiment.")
////                .putString("description", "Turn on your intervention now and use it for the next " + calculateInterval(schedule))
//////                .putString("description", "Please remember to <b>" + description + "</b> today.")
////                .build();
////
////        if ("daily".equalsIgnoreCase(schedule)) {
////            scheduleDailyNotifications(data, "morningNotification", 8, 0); // Schedule morning notification at 8 AM
////            scheduleDailyNotifications(data, "eveningNotification", 20, 0); // Schedule evening notification at 8 PM
////        } else if ("weekly".equalsIgnoreCase(schedule)) {
////            scheduleWeeklyNotifications(data, "weeklyMorningNotification", 8, 0); // Schedule morning notification weekly
////            scheduleWeeklyNotifications(data, "weeklyEveningNotification", 20, 0); // Schedule evening notification weekly
////        } else if ("every 2 days".equalsIgnoreCase(schedule)) {
////            scheduleEvery2DaysNotifications(data, "every2DaysMorningNotification", 8, 0); // Schedule morning notification every 2 days
////            scheduleEvery2DaysNotifications(data, "every2DaysEveningNotification", 20, 0); // Schedule evening notification every 2 days
////        } else if ("monthly".equalsIgnoreCase(schedule)) {
////            scheduleMonthlyNotifications(data, "monthlyMorningNotification", 8, 0); // Schedule morning notification monthly
////            scheduleMonthlyNotifications(data, "monthlyEveningNotification", 20, 0); // Schedule evening notification monthly
////        } else {
////            // Default to daily
////            scheduleDailyNotifications(data, "defaultMorningNotification", 8, 0); // Schedule morning notification at 8 AM
////            scheduleDailyNotifications(data, "defaultEveningNotification", 20, 0); // Schedule evening notification at 8 PM
////        }
////    }
//
//    // Cancel the notification worker
//    private void cancelNotificationWorker() {
//        WorkManager.getInstance(this).cancelAllWorkByTag("morningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("eveningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("weeklyMorningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("weeklyEveningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("every2DaysMorningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("every2DaysEveningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("monthlyMorningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("monthlyEveningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("defaultMorningNotification");
//        WorkManager.getInstance(this).cancelAllWorkByTag("defaultEveningNotification");
//        WorkManager.getInstance(this).cancelAllWork();
//    }
//
////    private void saveExperimentToFirestore(String title, String goal, String steps, String schedule, boolean isRunning) {
////        String deviceIdConcat = deviceId + "-" + DeviceModel;
////
////        Instant nowUtc = Instant.now();
////        long epochSeconds = nowUtc.getEpochSecond();
////
////        Clock utcClock = Clock.systemUTC();
////        LocalDate currentDateUTC = LocalDate.now(utcClock);
////
////        String documentId = "experiment_" + epochSeconds;
////
////        Map<String, Object> experiment = new HashMap<>();
////        experiment.put("title", title);
////        experiment.put("goal", goal);
////        experiment.put("steps", steps);
////        experiment.put("schedule", schedule);
////        experiment.put("isRunning", isRunning);
////        experiment.put("createdAt", epochSeconds);
////
//////      ---------------------------------experiment days---------------------------------
////
////        List<String> interventionDays = calculateInterventionDays(schedule, currentDateUTC);
////        for (String day : interventionDays) {
////            fetchAndSaveExperimentDays(deviceIdConcat, documentId, LocalDate.parse(day));
////        }
////
//////        Map<String, Object> experimentDaysArray = new HashMap<>();
//////        experimentDaysArray.put("isFullDay", false);
//////        experimentDaysArray.put("isInterventionDay", true);
//////        experimentDaysArray.put("date", currentDateUTC.toString());
//////
//////
//////        FireStoreDB.collection("Devices").document(deviceIdConcat)
//////                .collection("experiments").document(documentId)
//////                .collection("control_days").document("day_1")
//////                .set(experimentDaysArray)
//////                .addOnSuccessListener(aVoid -> {
//////                    Log.d("Firestore", "Experiment successfully written!");
//////                    saveControlDayInLocalStorage("control_days", currentDateUTC.toString());
//////                })
//////                .addOnFailureListener(e -> {
//////                    Log.e("Firestore", "Error writing document", e);
//////
//////                });
////        //      ---------------------------------end experiment days---------------------------------
////
////        FireStoreDB.collection("Devices").document(deviceIdConcat)
////                .collection("experiments").document(documentId)
////                .set(experiment)
////                .addOnSuccessListener(aVoid -> {
////                    Log.d("Firestore", "Experiment successfully written!");
////                    Toast.makeText(CreateExperiment.this, "Experiment Submitted Successfully", Toast.LENGTH_SHORT).show();
////                    if (isRunning) {
////                        scheduleNotificationWorker(title, goal, schedule);
////                    } else {
////                        cancelNotificationWorker();
////                    }
////                    hideLoadingScreen();
////                })
////                .addOnFailureListener(e -> {
////                    Log.e("Firestore", "Error writing document", e);
////                    Toast.makeText(CreateExperiment.this, "Failed to submit experiment", Toast.LENGTH_SHORT).show();
////                    hideLoadingScreen();
////                });
////
////
////    }
//
//
//    private void fetchAndSaveExperimentDays(String deviceIdConcat, String documentId, LocalDate currentDateUTC) {
//        FireStoreDB.collection("Devices").document(deviceIdConcat)
//                .collection("experiments").document(documentId)
//                .collection("control_days")
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        int nextDayNumber = 1;
//                        if (task.getResult() != null && !task.getResult().isEmpty()) {
//                            nextDayNumber = task.getResult().size() + 1;
//                        }
//
//                        Map<String, Object> experimentDaysArray = new HashMap<>();
//                        experimentDaysArray.put("isFullDay", false);
//                        experimentDaysArray.put("isInterventionDay", true);
//                        experimentDaysArray.put("date", currentDateUTC.toString());
//
//                        FireStoreDB.collection("Devices").document(deviceIdConcat)
//                                .collection("experiments").document(documentId)
//                                .collection("control_days").document("day_" + nextDayNumber)
//                                .set(experimentDaysArray)
//                                .addOnSuccessListener(aVoid -> {
//                                    Log.d("Firestore", "Experiment day successfully written!");
//                                    saveControlDayInLocalStorage("control_days", currentDateUTC.toString());
//                                })
//                                .addOnFailureListener(e -> {
//                                    Log.e("Firestore", "Error writing document", e);
//                                });
//                    } else {
//                        Log.e("Firestore", "Error getting documents: ", task.getException());
//                    }
//                });
//    }
//
//    private void saveControlDayInLocalStorage(String key, String day) {
//        SharedPreferences sharedPreferences = getSharedPreferences("ControlDays", MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        Set<String> days = sharedPreferences.getStringSet(key, new HashSet<>());
//        days.add(day);
//        editor.putStringSet(key, days);
//        editor.apply();
//    }
//    private Set<String> getDaysFromLocalStorage(String key) {
//        SharedPreferences sharedPreferences = getSharedPreferences("ControlDays", MODE_PRIVATE);
//        return sharedPreferences.getStringSet(key, new HashSet<>());
//    }
//
//
//
//    private void scheduleDailyNotifications(Data data, String workName, int hour, int minute) {
//        Calendar now = Calendar.getInstance();
//        Calendar notificationTime = Calendar.getInstance();
//        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
//        notificationTime.set(Calendar.MINUTE, minute);
//        notificationTime.set(Calendar.SECOND, 0);
//
//        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
//        if (initialDelay < 0) {
//            initialDelay += TimeUnit.DAYS.toMillis(1); // Schedule for the next day if the time has already passed today
//        }
//
//
//        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS)
//                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
//                .setInputData(data)
//                .addTag(workName)
//                .build();
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
//    }
//
//    private void scheduleWeeklyNotifications(Data data, String workName, int hour, int minute) {
//        Calendar now = Calendar.getInstance();
//        Calendar notificationTime = Calendar.getInstance();
//        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
//        notificationTime.set(Calendar.MINUTE, minute);
//        notificationTime.set(Calendar.SECOND, 0);
//
//        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
//        if (initialDelay < 0) {
//            initialDelay += TimeUnit.DAYS.toMillis(7); // Schedule for the next week if the time has already passed today
//        }
//
//        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 7, TimeUnit.DAYS)
//                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
//                .setInputData(data)
//                .addTag(workName)
//                .build();
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
//    }
//
//    private void scheduleEvery2DaysNotifications(Data data, String workName, int hour, int minute) {
//        Calendar now = Calendar.getInstance();
//        Calendar notificationTime = Calendar.getInstance();
//        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
//        notificationTime.set(Calendar.MINUTE, minute);
//        notificationTime.set(Calendar.SECOND, 0);
//
//        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
//        if (initialDelay < 0) {
//            initialDelay += TimeUnit.DAYS.toMillis(2); // Schedule for the next 2 days if the time has already passed today
//        }
//
//        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 2, TimeUnit.DAYS)
//                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
//                .setInputData(data)
//                .addTag(workName)
//                .build();
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
//    }
//
//    private void scheduleMonthlyNotifications(Data data, String workName, int hour, int minute) {
//        Calendar now = Calendar.getInstance();
//        Calendar notificationTime = Calendar.getInstance();
//        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
//        notificationTime.set(Calendar.MINUTE, minute);
//        notificationTime.set(Calendar.SECOND, 0);
//
//        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
//        if (initialDelay < 0) {
//            initialDelay += TimeUnit.DAYS.toMillis(30);
//        }
//
//        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 30, TimeUnit.DAYS)
//                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
//                .setInputData(data)
//                .addTag(workName)
//                .build();
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
//    }
//    private String calculateInterval(String schedule) {
//        switch (schedule.toLowerCase()) {
//            case "daily":
//                return "<b>" + TimeUnit.DAYS.toHours(1) + " hours</b>";
//            case "weekly":
//                return "<b>" + TimeUnit.DAYS.toDays(7) + " days</b>";
//            case "every 2 days":
//                return "<b>" + TimeUnit.DAYS.toHours(2) + " hours</b>";
//            case "monthly":
//                return "<b>" + TimeUnit.DAYS.toDays(30) + " days</b>";
//            default:
//                // Default to daily
//                return "<b>" + TimeUnit.DAYS.toHours(1) + " hours</b>";
//        }
//    }
//
//
//
//    private void fetchLastExperiment() {
//        showLoadingScreen();
//
//        String deviceIdConcat = deviceId + "-" + DeviceModel;
//
//        FireStoreDB.collection("Devices").document(deviceIdConcat)
//                .collection("experiments")
//                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
//                .limit(1) // Limit to only the most recent document
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
//                        DocumentSnapshot lastExperiment = task.getResult().getDocuments().get(0);
//                        String title = lastExperiment.getString("title");
//                        String goal = lastExperiment.getString("goal");
//                        String steps = lastExperiment.getString("steps");
//                        String schedule = lastExperiment.getString("schedule");
//                        Boolean isRunning = lastExperiment.getBoolean("isRunning");
//
//
//                        // Example of setting values to UI elements (make sure this runs on the UI thread if it's not already)
//                        runOnUiThread(() -> {
//
//                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) scheduleSpinner.getAdapter();
//                            int position = adapter.getPosition(schedule); // Get the position of the item in the adapter
//
//                            experimentTitleInput.setText(title);
//                            experimentGoalInput.setText(goal);
//                            stepsTakenInput.setText(steps);
//                            scheduleSpinner.setSelection(position);
//                            runningSwitch.setChecked(isRunning != null && isRunning);
//                        });
//
//                    } else {
//                        Log.d("Firestore", "No experiments found or failed to fetch the data.");
//                    }
//                    hideLoadingScreen();
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("Firestore", "Error fetching document", e);
//                    hideLoadingScreen();
//                });
//    }
//
//    private List<String> calculateInterventionDays(String schedule, LocalDate startDate) {
//        List<String> interventionDays = new ArrayList<>();
//        int interval = getIntervalFromSchedule(schedule);
//
//        for (int i = 1; i <= 10; i++) { // Example for 10 days
//            interventionDays.add(startDate.plusDays(i * interval).toString());
//        }
//        return interventionDays;
//    }
//    private int getIntervalFromSchedule(String schedule) {
//        switch (schedule) {
//            case "Daily":
//                return 1;
//            case "Every 2 Days":
//                return 2;
//            case "Weekly":
//                return 7;
//            case "Monthly":
//                return 30;
//            default:
//                return 1; // Default to daily if the schedule is not recognized
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.logout_option, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.menu_logout) {
//            AuthenticationUtils.logoutUser(this);
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    //================================================================================================
//
//    private void saveExperimentToFirestore(String title, String goal, String steps, String schedule, boolean isRunning) {
//        String deviceIdConcat = deviceId + "-" + DeviceModel;
//
//        Instant nowUtc = Instant.now();
//        long epochSeconds = nowUtc.getEpochSecond();
//
//        Clock utcClock = Clock.systemUTC();
//        LocalDate currentDateUTC = LocalDate.now(utcClock);
//
//        String documentId = "experiment_" + epochSeconds;
//
//        Map<String, Object> experiment = new HashMap<>();
//        experiment.put("title", title);
//        experiment.put("goal", goal);
//        experiment.put("steps", steps);
//        experiment.put("schedule", schedule);
//        experiment.put("isRunning", isRunning);
//        experiment.put("createdAt", epochSeconds);
//
//        FireStoreDB.collection("Devices").document(deviceIdConcat)
//                .collection("experiments").document(documentId)
//                .set(experiment)
//                .addOnSuccessListener(aVoid -> {
//                    Log.d("Firestore", "Experiment successfully written!");
//                    Toast.makeText(CreateExperiment.this, "Experiment Submitted Successfully", Toast.LENGTH_SHORT).show();
//                    if (isRunning) {
//                        scheduleNotificationWorker(title, goal, schedule);
//                    } else {
//                        cancelNotificationWorker();
//                    }
//                    hideLoadingScreen();
//                    // Initialize the experiment days
//                    initializeExperimentDays(deviceIdConcat, documentId, currentDateUTC, schedule);
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("Firestore", "Error writing document", e);
//                    Toast.makeText(CreateExperiment.this, "Failed to submit experiment", Toast.LENGTH_SHORT).show();
//                    hideLoadingScreen();
//                });
//    }
//
//    private void initializeExperimentDays(String deviceIdConcat, String documentId, LocalDate startDate, String schedule) {
//        // Add the initial partial day (not counted as full day)
//        saveExperimentDay(deviceIdConcat, documentId, startDate.minusDays(1), "partial_control");
//
//        // Determine the schedule and set up the days accordingly
//        if ("Daily".equals(schedule)) {
//            setupDailySchedule(deviceIdConcat, documentId, startDate);
//        } else if ("Every 2 Days".equals(schedule)) {
//            setupEvery2DaysSchedule(deviceIdConcat, documentId, startDate);
//        }
//    }
//
//    private void saveExperimentDay(String deviceIdConcat, String documentId, LocalDate date, String dayType) {
//        Map<String, Object> experimentDaysArray = new HashMap<>();
//        experimentDaysArray.put("isFullDay", !"partial_control".equals(dayType));
//        experimentDaysArray.put("isInterventionDay", "intervention".equals(dayType));
//        experimentDaysArray.put("date", date.toString());
//
//        FireStoreDB.collection("Devices").document(deviceIdConcat)
//                .collection("experiments").document(documentId)
//                .collection(dayType + "_days").document("day_" + date.toString())
//                .set(experimentDaysArray)
//                .addOnSuccessListener(aVoid -> {
//                    Log.d("Firestore", "Experiment day successfully written!");
//                    saveDayInLocalStorage(dayType + "_days", date.toString());
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("Firestore", "Error writing document", e);
//                });
//    }
//
//    private void saveDayInLocalStorage(String key, String day) {
//        SharedPreferences sharedPreferences = getSharedPreferences("ExperimentDays", MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        Set<String> days = sharedPreferences.getStringSet(key, new HashSet<>());
//        days.add(day);
//        editor.putStringSet(key, days);
//        editor.apply();
//    }
//
//    private void scheduleNotificationWorker(String title, String description, String schedule) {
//        if ("daily".equalsIgnoreCase(schedule)) {
//            scheduleDailyNotifications(title, schedule);
//        } else if ("every 2 days".equalsIgnoreCase(schedule)) {
//            scheduleEvery2DaysNotifications(title, schedule);
//        } else if ("weekly".equalsIgnoreCase(schedule)) {
////            scheduleWeeklyNotifications(title, schedule);
//        } else if ("monthly".equalsIgnoreCase(schedule)) {
////            scheduleMonthlyNotifications(title, schedule);
//        } else {
//            // Default to daily
//            scheduleDailyNotifications(title, schedule);
//        }
//    }
//
//    private void scheduleDailyNotifications(String title, String schedule) {
//        for (int i = 1; i <= 3; i++) { // Example for 3 days
//            if (i % 2 == 1) { // Control days
//                scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title, schedule);
//            } else { // Intervention days
//                scheduleNotificationForDay("Morning", LocalDate.now().plusDays(i - 1), "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title, schedule);
//                scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be a CONTROL DAY.", title, schedule);
//            }
//        }
//    }
//
//    private void scheduleEvery2DaysNotifications(String title, String schedule) {
//        for (int i = 1; i <= 4; i++) { // Example for 4 days
//            if (i % 4 == 1 || i % 4 == 2) { // Control days
//                if (i % 4 == 2) {
//                    scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title, schedule);
//                }
//            } else { // Intervention days
//                if (i % 4 == 3) {
//                    scheduleNotificationForDay("Morning", LocalDate.now().plusDays(i - 1), "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title, schedule);
//                } else {
//                    scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be a CONTROL DAY.", title, schedule);
//                }
//            }
//        }
//    }
//
//    private void scheduleNotificationForDay(String time, LocalDate date, String message, String title, String schedule) {
//        Data data = new Data.Builder()
//                .putString("title", title)
//                .putString("description", message)
//                .build();
//
//        Calendar now = Calendar.getInstance();
//        Calendar notificationTime = Calendar.getInstance();
//        notificationTime.set(Calendar.YEAR, date.getYear());
//        notificationTime.set(Calendar.MONTH, date.getMonthValue() - 1); // Months are 0-based in Calendar
//        notificationTime.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
//
//        if ("Morning".equalsIgnoreCase(time)) {
//            notificationTime.set(Calendar.HOUR_OF_DAY, 8); // 8 AM
//            notificationTime.set(Calendar.MINUTE, 0);
//        } else if ("Evening".equalsIgnoreCase(time)) {
//            notificationTime.set(Calendar.HOUR_OF_DAY, 20); // 8 PM
//            notificationTime.set(Calendar.MINUTE, 0);
//        }
//
//        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
//        if (initialDelay < 0) {
//            initialDelay += TimeUnit.DAYS.toMillis(1); // Schedule for the next day if the time has already passed today
//        }
//
//        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS)
//                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
//                .setInputData(data)
//                .addTag(time + "Notification" + date.toString()) // Unique tag for each notification
//                .build();
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(time + "Notification" + date.toString(), ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
//    }
//
//
//
//}

package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

import com.example.myapplication.Util.AuthenticationUtils;
import com.example.myapplication.Worker.NotificationWorker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CreateExperiment extends AppCompatActivity {
    private EditText experimentTitleInput;
    private EditText experimentGoalInput;
    private EditText stepsTakenInput;

    private Spinner scheduleSpinner;
    private SwitchMaterial runningSwitch;

    private String deviceId;
    private String DeviceModel;
    private View loadingScreen;
    private FirebaseFirestore FireStoreDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_experiment);

        FireStoreDB = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.top_app_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Create Experiment");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });

        if (deviceId == null) {
            deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        if (DeviceModel == null) {
            DeviceModel = Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
        }

        experimentTitleInput = findViewById(R.id.experiment_title);
        experimentGoalInput = findViewById(R.id.experiment_goal);
        stepsTakenInput = findViewById(R.id.steps_taken);
        scheduleSpinner = findViewById(R.id.schedule_spinner);
        runningSwitch = findViewById(R.id.running_switch);
        loadingScreen = findViewById(R.id.loading_screen);

        fetchLastExperiment();

        // Button initialization and setOnClickListener
        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitExperiment();
            }
        });
    }

    private void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
    }

    private void submitExperiment() {
        showLoadingScreen();
        // Retrieving the text from EditText fields
        String title = experimentTitleInput.getText().toString();
        String goal = experimentGoalInput.getText().toString();
        String steps = stepsTakenInput.getText().toString();
        String schedule = scheduleSpinner.getSelectedItem().toString();
        boolean isRunning = runningSwitch.isChecked();

        if (title.isEmpty() || goal.isEmpty() || steps.isEmpty()) {
            Toast.makeText(CreateExperiment.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            hideLoadingScreen();
        } else {
            saveExperimentToFirestore(title, goal, steps, schedule, isRunning);
        }
    }

    private void saveExperimentToFirestore(String title, String goal, String steps, String schedule, boolean isRunning) {
        String deviceIdConcat = deviceId + "-" + DeviceModel;

        Instant nowUtc = Instant.now();
        long epochSeconds = nowUtc.getEpochSecond();

        Clock utcClock = Clock.systemUTC();
        LocalDate currentDateUTC = LocalDate.now(utcClock);

        String documentId = "experiment_" + epochSeconds;

        Map<String, Object> experiment = new HashMap<>();
        experiment.put("title", title);
        experiment.put("goal", goal);
        experiment.put("steps", steps);
        experiment.put("schedule", schedule);
        experiment.put("isRunning", isRunning);
        experiment.put("createdAt", epochSeconds);

        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments").document(documentId)
                .set(experiment)
                .addOnSuccessListener(aVoid -> {
                    cancelNotificationWorker();
                    Log.d("Firestore", "Experiment successfully written!");
                    Toast.makeText(CreateExperiment.this, "Experiment Submitted Successfully", Toast.LENGTH_SHORT).show();
                    if (isRunning) {
//                        scheduleNotificationWorker(title, schedule);
                        initializeExperimentDays(deviceIdConcat, documentId, currentDateUTC, schedule, title);
                    }
                    hideLoadingScreen();

                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error writing document", e);
                    Toast.makeText(CreateExperiment.this, "Failed to submit experiment", Toast.LENGTH_SHORT).show();
                    cancelNotificationWorker();
                    hideLoadingScreen();
                });
    }

    private void initializeExperimentDays(String deviceIdConcat, String documentId, LocalDate startDate, String schedule, String title) {
        // Add the initial partial day (not counted as full day)
//        saveExperimentDay(deviceIdConcat, documentId, startDate.minusDays(1), "partial_control");

        // Determine the schedule and set up the days accordingly
        if ("Daily".equals(schedule)) {
            setupDailySchedule(deviceIdConcat, documentId, startDate, title);
        } else if ("Every 2 Days".equals(schedule)) {
            setupEvery2DaysSchedule(deviceIdConcat, documentId, startDate, title);
        } else if ("Weekly".equals(schedule)) {
            setupWeeklySchedule(deviceIdConcat, documentId, startDate, title);
        } else {
            setupDailySchedule(deviceIdConcat, documentId, startDate, title);
        }
    }

    private void setupDailySchedule(String deviceIdConcat, String documentId, LocalDate startDate, String title) {
        for (int i = 1; i <= 30; i++) {
            LocalDate currentDate = startDate.plusDays(i - 1);
            if (i % 2 == 1) { // Control days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "control");
                scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "control");
//                if (i > 1) {
//                    scheduleNotificationForDay("Evening", currentDate.minusDays(1), "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title);
//                }
            } else { // Intervention days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "intervention");
                scheduleNotificationForDay("Morning", currentDate, "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title, documentId, deviceIdConcat, "intervention");
                scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be a CONTROL DAY.",  title, documentId, deviceIdConcat, "intervention");
            }
        }
    }

    private void setupEvery2DaysSchedule(String deviceIdConcat, String documentId, LocalDate startDate, String title) {
        for (int i = 1; i <= 30; i++)  {
            LocalDate currentDate = startDate.plusDays(i - 1);
            if (i % 4 == 1 || i % 4 == 2) { // Control days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "control");
                if (i % 4 == 2) {
                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "control");
                }
            } else { // Intervention days i % 4 == 3 || i % 4 == 0
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "intervention");
                scheduleNotificationForDay("Morning", currentDate, "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title, documentId, deviceIdConcat, "intervention");
                if (i % 4 == 3) {
                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "intervention");
                } else {
                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be a CONTROL DAY.", title, documentId, deviceIdConcat, "intervention");
                }
            }
        }
    }

    private void setupWeeklySchedule(String deviceIdConcat, String documentId, LocalDate startDate, String title) {
        for (int i = 1; i <= 30; i++) {
            LocalDate currentDate = startDate.plusDays(i - 1);

            if (i % 14 >= 1 && i % 14 <= 7) { // Control days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "control");
            } else { // Intervention days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "intervention");

                if (i % 14 == 8) { // First day of intervention period
                    scheduleNotificationForDay("Morning", currentDate, "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title, documentId, deviceIdConcat, "intervention");
                }

                if (i % 14 >= 9 && i % 14 <= 14) {
                    scheduleNotificationForDay("Morning", currentDate, "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title, documentId, deviceIdConcat, "intervention");
                    scheduleNotificationForDay("Evening", currentDate.minusDays(1), "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "intervention");
                }
            }
        }
    }



    private void saveExperimentDay(String deviceIdConcat, String documentId, LocalDate date, String dayType) {
        Map<String, Object> experimentDaysArray = new HashMap<>();
        experimentDaysArray.put("isFullDay", !"partial_control".equals(dayType));
        experimentDaysArray.put("isInterventionDay", "intervention".equals(dayType));
        experimentDaysArray.put("date", date.toString());
        experimentDaysArray.put("notificationsFulfilled", false);

        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments").document(documentId)
                .collection(dayType + "_days").document("day_" + date.toString())
                .set(experimentDaysArray)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Experiment day successfully written!");
                    saveDayInLocalStorage(dayType + "_days", date.toString());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error writing document", e);
                });
    }

    private void saveDayInLocalStorage(String key, String day) {
        SharedPreferences sharedPreferences = getSharedPreferences("ExperimentDays", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> days = sharedPreferences.getStringSet(key, new HashSet<>());
        days.add(day);
        editor.putStringSet(key, days);
        editor.apply();
    }

//    private void scheduleNotificationWorker(String title, String schedule) {
//        if ("daily".equalsIgnoreCase(schedule)) {
//            scheduleDailyNotifications(title, schedule);
//        } else if ("every 2 days".equalsIgnoreCase(schedule)) {
//            scheduleEvery2DaysNotifications(title, schedule);
//        } else {
//            // Default to daily
//            scheduleDailyNotifications(title, schedule);
//        }
//    }

//    private void scheduleDailyNotifications(String title, String schedule) {
//        for (int i = 1; i <= 3; i++) { // Example for 3 days
//            if (i % 2 == 1) { // Control days
//                scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title);
//            } else { // Intervention days
//                scheduleNotificationForDay("Morning", LocalDate.now().plusDays(i - 1), "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title);
//                scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be a CONTROL DAY.", title);
//            }
//        }
//    }
//
//    private void scheduleEvery2DaysNotifications(String title, String schedule) {
//        for (int i = 1; i <= 4; i++) { // Example for 4 days
//            if (i % 4 == 1 || i % 4 == 2) { // Control days
//                if (i % 4 == 2) {
//                    scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title);
//                }
//            } else { // Intervention days
//                if (i % 4 == 3) {
//                    scheduleNotificationForDay("Morning", LocalDate.now().plusDays(i - 1), "TODAY is an INTERVENTION DAY. Make sure you have your notifications on!", title);
//                } else {
//                    scheduleNotificationForDay("Evening", LocalDate.now().plusDays(i - 1), "TOMORROW will be a CONTROL DAY.", title);
//                }
//            }
//        }
//    }

    private void scheduleNotificationForDay(String time, LocalDate date, String message, String title, String documentId, String deviceIdConcat, String dayType) {
        Data data = new Data.Builder()
                .putString("title", title)
                .putString("description", message)
                .putString("deviceIdConcat", deviceIdConcat)
                .putString("documentId", documentId)
                .putString("dayType", dayType)
                .putString("date", date.toString())
                .build();

        Calendar now = Calendar.getInstance();
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.set(Calendar.YEAR, date.getYear());
        notificationTime.set(Calendar.MONTH, date.getMonthValue() - 1); // Months are 0-based in Calendar
        notificationTime.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());

        if ("Morning".equalsIgnoreCase(time)) {
            notificationTime.set(Calendar.HOUR_OF_DAY, 8); // 8 AM
            notificationTime.set(Calendar.MINUTE, 0);
        } else if ("Evening".equalsIgnoreCase(time)) {
            notificationTime.set(Calendar.HOUR_OF_DAY, 11); // 8 PM
            notificationTime.set(Calendar.MINUTE, 46);
        }

        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(1); // Schedule for the next day if the time has already passed today
        }

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(time + "Notification" + date.toString()) // Unique tag for each notification
                .build();
        Log.e("scheduleNotificationForDay", time + "Notification" + date.toString() + " " +  message);
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(time + "Notification" + date.toString(), ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
    }

    private void cancelNotificationWorker() {
        WorkManager.getInstance(this).cancelAllWork();
    }

    private void fetchLastExperiment() {
        showLoadingScreen();

        String deviceIdConcat = deviceId + "-" + DeviceModel;

        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments")
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(1) // Limit to only the most recent document
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot lastExperiment = task.getResult().getDocuments().get(0);
                        String title = lastExperiment.getString("title");
                        String goal = lastExperiment.getString("goal");
                        String steps = lastExperiment.getString("steps");
                        String schedule = lastExperiment.getString("schedule");
                        Boolean isRunning = lastExperiment.getBoolean("isRunning");

                        // Example of setting values to UI elements (make sure this runs on the UI thread if it's not already)
                        runOnUiThread(() -> {
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) scheduleSpinner.getAdapter();
                            int position = adapter.getPosition(schedule); // Get the position of the item in the adapter

                            experimentTitleInput.setText(title);
                            experimentGoalInput.setText(goal);
                            stepsTakenInput.setText(steps);
                            scheduleSpinner.setSelection(position);
                            runningSwitch.setChecked(isRunning != null && isRunning);
                        });

                    } else {
                        Log.d("Firestore", "No experiments found or failed to fetch the data.");
                    }
                    hideLoadingScreen();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching document", e);
                    hideLoadingScreen();
                });
    }

    private int getIntervalFromSchedule(String schedule) {
        switch (schedule) {
            case "Daily":
                return 1;
            case "Every 2 Days":
                return 2;
            case "Weekly":
                return 7;
            case "Monthly":
                return 30;
            default:
                return 1; // Default to daily if the schedule is not recognized
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logout_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_logout) {
            AuthenticationUtils.logoutUser(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
