package com.example.myapplication;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// notification worker
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Data;

import java.util.concurrent.TimeUnit;

public class CreateExperiment extends AppCompatActivity {
    private EditText experimentTitleInput;
    private EditText experimentGoalInput;
    private EditText stepsTakenInput;

    private Spinner scheduleSpinner;
    private SwitchMaterial runningSwitch;

    private String deviceId;

    String DeviceModel;
    private View loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_experiment);

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
        if (DeviceModel ==  null){
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

//        FirebaseMessaging.getInstance().getToken()
//                .addOnCompleteListener(new OnCompleteListener<String>() {
//                    @Override
//                    public void onComplete(@NonNull Task<String> task) {
//                        if (!task.isSuccessful()) {
//                            Log.w("fcm", "Fetching FCM registration token failed", task.getException());
//                            return;
//                        }
//
//                        // Get new FCM registration token
//                        String token = task.getResult();
//
//                        // Log and toast
//
//                        Log.d("fcm", token);
//                        Toast.makeText(CreateExperiment.this, "fcm", Toast.LENGTH_SHORT).show();
//                    }
//                });

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

        if(title.isEmpty() || goal.isEmpty() || steps.isEmpty()) {
            Toast.makeText(CreateExperiment.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            hideLoadingScreen();
        } else {
            saveExperimentToFirestore(title, goal, steps, schedule, isRunning);
            Toast.makeText(CreateExperiment.this, "Experiment Submitted Successfully", Toast.LENGTH_SHORT).show();

        }
    }
    // Schedule a notification worker to run every 12 hours
//    private void scheduleNotificationWorker(String title, String description, String schedule) {
//        Data data = new Data.Builder()
//                .putString("title", title)
//                .putString("description", description)
//                .build();
//
//        long repeatInterval = calculateRepeatInterval(schedule);
//
//        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, repeatInterval, TimeUnit.MILLISECONDS)
//                .setInputData(data)
//                .build();
//
//        WorkManager.getInstance(this).enqueue(workRequest);
//    }
    private void scheduleNotificationWorker(String title, String description, String schedule) {
        Data data = new Data.Builder()
                .putString("title", "Today is an INTERVENTION DAY for your <i>" + title + "</i> experiment.")
                .putString("description", "Please remember to <b>" + description + "</b> today.")
                .build();

        if ("daily".equalsIgnoreCase(schedule)) {
            scheduleDailyNotifications(data, "morningNotification", 8, 0); // Schedule morning notification at 8 AM
            scheduleDailyNotifications(data, "eveningNotification", 20, 0); // Schedule evening notification at 8 PM
        } else if ("weekly".equalsIgnoreCase(schedule)) {
            scheduleWeeklyNotifications(data, "weeklyMorningNotification", 8, 0); // Schedule morning notification weekly
            scheduleWeeklyNotifications(data, "weeklyEveningNotification", 20, 0); // Schedule evening notification weekly
        } else if ("every 2 days".equalsIgnoreCase(schedule)) {
            scheduleEvery2DaysNotifications(data, "every2DaysMorningNotification", 8, 0); // Schedule morning notification every 2 days
            scheduleEvery2DaysNotifications(data, "every2DaysEveningNotification", 20, 0); // Schedule evening notification every 2 days
        } else if ("monthly".equalsIgnoreCase(schedule)) {

        } else {
            // Default to daily
            scheduleDailyNotifications(data, "defaultMorningNotification", 8, 0); // Schedule morning notification at 8 AM
            scheduleDailyNotifications(data, "defaultEveningNotification", 20, 0); // Schedule evening notification at 8 PM
        }
    }

    // Cancel the notification worker
    private void cancelNotificationWorker() {
        WorkManager.getInstance(this).cancelAllWorkByTag("morningNotification");
        WorkManager.getInstance(this).cancelAllWorkByTag("eveningNotification");
        WorkManager.getInstance(this).cancelAllWorkByTag("weeklyMorningNotification");
        WorkManager.getInstance(this).cancelAllWorkByTag("weeklyEveningNotification");
        WorkManager.getInstance(this).cancelAllWorkByTag("every2DaysMorningNotification");
        WorkManager.getInstance(this).cancelAllWorkByTag("every2DaysEveningNotification");
        WorkManager.getInstance(this).cancelAllWorkByTag("defaultMorningNotification");
        WorkManager.getInstance(this).cancelAllWorkByTag("defaultEveningNotification");
        WorkManager.getInstance(this).cancelAllWork();
    }

    private void saveExperimentToFirestore(String title, String goal, String steps, String schedule, boolean isRunning) {
        String deviceIdConcat = deviceId + "-" + DeviceModel;

        Instant nowUtc = Instant.now();
        long epochSeconds = nowUtc.getEpochSecond();

        String documentId = "experiment_" + epochSeconds;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> experiment = new HashMap<>();
        experiment.put("title", title);
        experiment.put("goal", goal);
        experiment.put("steps", steps);
        experiment.put("schedule", schedule);
        experiment.put("isRunning", isRunning);
        experiment.put("createdAt", epochSeconds);

        db.collection("Devices").document(deviceIdConcat)
                .collection("experiments").document(documentId)
                .set(experiment)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Experiment successfully written!");
                    Toast.makeText(CreateExperiment.this, "Experiment Submitted Successfully", Toast.LENGTH_SHORT).show();
                    if (isRunning) {
                        scheduleNotificationWorker(title, goal, schedule);
                    } else {
                        cancelNotificationWorker();
                    }
                    hideLoadingScreen();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error writing document", e);
                    Toast.makeText(CreateExperiment.this, "Failed to submit experiment", Toast.LENGTH_SHORT).show();
                    hideLoadingScreen();
                });

    }

    private void scheduleDailyNotifications(Data data, String workName, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
        notificationTime.set(Calendar.MINUTE, minute);
        notificationTime.set(Calendar.SECOND, 0);

        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(1); // Schedule for the next day if the time has already passed today
        }


        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(workName)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
    }

    private void scheduleWeeklyNotifications(Data data, String workName, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
        notificationTime.set(Calendar.MINUTE, minute);
        notificationTime.set(Calendar.SECOND, 0);

        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(7); // Schedule for the next week if the time has already passed today
        }

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(workName)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
    }

    private void scheduleEvery2DaysNotifications(Data data, String workName, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
        notificationTime.set(Calendar.MINUTE, minute);
        notificationTime.set(Calendar.SECOND, 0);

        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(2); // Schedule for the next 2 days if the time has already passed today
        }

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 2, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(workName)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
    }

    private void scheduleMonthlyNotifications(Data data, String workName, int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar notificationTime = Calendar.getInstance();
        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
        notificationTime.set(Calendar.MINUTE, minute);
        notificationTime.set(Calendar.SECOND, 0);

        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(30); // Schedule for the next week if the time has already passed today
        }

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 30, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(workName)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest);
    }
    private long calculateRepeatInterval(String schedule) {
        switch (schedule.toLowerCase()) {
            case "daily":
                return TimeUnit.DAYS.toMillis(1);
            case "weekly":
                return TimeUnit.DAYS.toMillis(7);
            case "every 2 days":
                return TimeUnit.DAYS.toMillis(2);
            case "monthly":
                return TimeUnit.DAYS.toMillis(30);
            default:
                // Default to daily
                return TimeUnit.DAYS.toMillis(1);
        }
    }



    private void fetchLastExperiment() {
        showLoadingScreen();

        String deviceIdConcat = deviceId + "-" + DeviceModel;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Devices").document(deviceIdConcat)
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
