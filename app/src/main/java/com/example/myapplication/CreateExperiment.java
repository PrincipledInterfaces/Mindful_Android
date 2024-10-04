package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

import com.example.myapplication.Model.Experiment;
import com.example.myapplication.Util.AuthenticationUtils;
import com.example.myapplication.Worker.NotificationWorker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
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

    private List<Experiment> experimentsList;
    private Experiment selectedExperiment;

    private FirebaseAuth auth;
    private String fid;
    private EditText experimentTitleInput;
    private EditText experimentGoalInput;
    private EditText stepsTakenInput;

    private Spinner scheduleSpinner;
    private Spinner durationSpinner;
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
        loadExperimentsData();

        Intent intent = getIntent();
        if (intent != null) {
            fid = intent.getStringExtra("fid");
            if (fid == null){
                fid = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            }

            int experimentId = intent.getIntExtra("experiment_id", -1);  // -1 is the default if not found
            if (experimentId == -1) {
                Log.e("CreateExperiment", "No Predefined Experiment id");
            } else {
                if (experimentsList != null && !experimentsList.isEmpty()) {
                    selectedExperiment = getExperimentById(experimentId);
                }
            }
        }

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
//            deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            deviceId = fid;
        }
        if (DeviceModel == null) {
            DeviceModel = Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
        }

        experimentTitleInput = findViewById(R.id.experiment_title);
        experimentGoalInput = findViewById(R.id.experiment_goal);
        stepsTakenInput = findViewById(R.id.steps_taken);
        scheduleSpinner = findViewById(R.id.schedule_spinner);
        durationSpinner = findViewById(R.id.duration_spinner);
//        runningSwitch = findViewById(R.id.running_switch);
        loadingScreen = findViewById(R.id.loading_screen);

        if (selectedExperiment != null){
            setDefaultValues();
        }
        else {
            fetchLastExperiment();
        }

        setDefaultScheduleValue();
        setDefaultDurationValue();
        setHelpDialog();



        // Button initialization and setOnClickListener
        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitExperiment();
            }
        });


    }

    private void setDefaultValues() {
        experimentTitleInput.setText(selectedExperiment.title);
        experimentGoalInput.setText(selectedExperiment.goal);
        stepsTakenInput.setText(android.text.TextUtils.join("\n", selectedExperiment.steps));
    }

    private void loadExperimentsData() {
    try {
        // Open the raw resource
        InputStream inputStream = getResources().openRawResource(R.raw.predefined_experiments);

        // Use an InputStreamReader to read the file
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);

        // Read the JSON file into a StringBuilder
        StringBuilder jsonStringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            jsonStringBuilder.append(line);
        }

        // Log the raw JSON content (optional, for debugging)
        Log.d("JSON Data", jsonStringBuilder.toString());

        // Now parse the JSON using Gson
        Type experimentListType = new TypeToken<List<Experiment>>() {}.getType();
        experimentsList = new Gson().fromJson(jsonStringBuilder.toString(), experimentListType);

        if (experimentsList == null) {
            Log.e("ErrorJson", "Parsed list is null.");
        } else {
            Log.d("Success", "Experiments loaded successfully.");
        }
    } catch (Resources.NotFoundException e) {
        Log.e("ErrorJson", "Resource not found: " + e.getMessage());
    } catch (IOException e) {
        Log.e("ErrorJson", "Error reading the file: " + e.getMessage());
    } catch (JsonSyntaxException e) {
        Log.e("ErrorJson", "Error parsing JSON: " + e.getMessage());
    }
    }


    private Experiment getExperimentById(int id) {
        if (experimentsList == null) {
            Log.e("Error", "Experiment list is null.");
            return null;
        }

        for (Experiment experiment : experimentsList) {
            if (experiment.id == id) {
                return experiment;
            }
        }
        return null;  // Return null if no experiment is found with the given ID
    }


    private void setHelpDialog() {
        findViewById(R.id.schedule_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(CreateExperiment.this)
                        .setTitle("Help with Scheduling")
                        .setMessage("You'll spend your chosen number of days in each condition. It's crucial to have 'normal' phone use to compare against your usage during the test to see any effects. We'll notify you the evening before and the morning after you need to switch your intervention on or off to keep you on track!")
                        .setPositiveButton(R.string.modal_btn_text, null)
                        .show();
            }
        });

        findViewById(R.id.duration_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(CreateExperiment.this)
                        .setTitle("Help with Duration")
                        .setMessage("Your test will run for the number of weeks you select. It's crucial to run the test long enough to capture your phone habits both with and without the intervention across all days of the week. This ensures we account for variations in phone usage by day (e.g., Monday, Tuesday). Running the test for several weeks helps us determine that any changes in your phone usage are due to the intervention and not other factors.")
                        .setPositiveButton(R.string.modal_btn_text, null)
                        .show();
            }
        });
    }

    private void setDefaultScheduleValue() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.schedule_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scheduleSpinner.setAdapter(adapter);

        scheduleSpinner.setSelection(1);
    }

    private void setDefaultDurationValue() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.duration_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(adapter);

        durationSpinner.setSelection(1);
    }

    private void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
    }

    private void submitExperiment() {
        showLoadingScreen();

        fetchAndDisableLastExperiment(() -> {
            // Retrieving the text from EditText fields
            String title = experimentTitleInput.getText().toString();
            String goal = experimentGoalInput.getText().toString();
            String steps = stepsTakenInput.getText().toString();
            String schedule = scheduleSpinner.getSelectedItem().toString();
            String duration = durationSpinner.getSelectedItem().toString();
            boolean isRunning = true;

            if (title.isEmpty() || goal.isEmpty() || steps.isEmpty()) {
                Toast.makeText(CreateExperiment.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
                hideLoadingScreen();
            } else {
                saveExperimentToFirestore(title, goal, steps, schedule, duration, isRunning);
            }
        });
    }

    private void saveExperimentToFirestore(String title, String goal, String steps, String schedule,String duration , boolean isRunning) {
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
        experiment.put("duration", duration);
        experiment.put("isRunning", isRunning);
        experiment.put("createdAt", epochSeconds);

        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments").document(documentId)
                .set(experiment)
                .addOnSuccessListener(aVoid -> {
                    cancelNotificationWorker();
                    Log.d("Firestore", "Experiment successfully written!");
                    if (isRunning) {
                        initializeExperimentDays(deviceIdConcat, documentId, currentDateUTC, schedule, duration, title);
                        showSuccessDialog(true);
                    } else {
                        showSuccessDialog(false);
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

    private void showSuccessDialog(boolean isRunning) {

        MaterialAlertDialogBuilder builder;
        AlertDialog dialog;
        if (isRunning) {
            builder = new MaterialAlertDialogBuilder(CreateExperiment.this);

            builder.setTitle("Experiment Submitted Successfully")
                    .setMessage("Your experiment has begun! Today is a CONTROL DAY, so please refrain from using your intervention. We'll send you notifications whenever it's time to turn your intervention on or off according to your selected schedule.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Redirect to MainActivity when OK is clicked
                            Intent intent = new Intent(CreateExperiment.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", null);

            dialog = builder.create();

        } else {
            dialog = new AlertDialog.Builder(CreateExperiment.this)
                    .setTitle("Experiment Submitted Successfully")
                    .setMessage("Your experiment has been saved, but it is not currently running. To start the experiment, toggle the switch to 'ON' and click the submit button again.\n\nGood luck!")
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        dialog.setOnDismissListener(dialogInterface -> {
            setResult(RESULT_OK);
        });

        dialog.show();
        hideLoadingScreen();
    }

    private void initializeExperimentDays(String deviceIdConcat, String documentId, LocalDate startDate, String schedule, String duration, String title) {
        // Add the initial partial day (not counted as full day)
//        saveExperimentDay(deviceIdConcat, documentId, startDate.minusDays(1), "partial_control");

        // Determine the schedule and set up the days accordingly
        if ("Daily".equals(schedule)) {
            setupDailySchedule(deviceIdConcat, documentId, startDate, title, getIntervalFromDuration(duration));
        } else if ("Every 2 Days".equals(schedule)) {
            setupEvery2DaysSchedule(deviceIdConcat, documentId, startDate, title, getIntervalFromDuration(duration));
        } else if ("Weekly".equals(schedule)) {
            setupWeeklySchedule(deviceIdConcat, documentId, startDate, title, getIntervalFromDuration(duration));
        } else {
            setupDailySchedule(deviceIdConcat, documentId, startDate, title, getIntervalFromDuration(duration));
        }
    }

    private void setupDailySchedule(String deviceIdConcat, String documentId, LocalDate startDate, String title, int duration) {
        for (int i = 1; i <= duration; i++) {
            LocalDate currentDate = startDate.plusDays(i - 1);
            if (i % 2 == 1) { // Control days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "control");
                scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be an <font color='#FF0000'>INTERVENTION DAY</font>. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "control");
//                if (i > 1) {
//                    scheduleNotificationForDay("Evening", currentDate.minusDays(1), "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title);
//                }
            } else { // Intervention days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "intervention");
                scheduleNotificationForDay("Morning", currentDate, "TODAY is an <font color='#FF0000'>INTERVENTION DAY</font>. Make sure you have your notifications on!", title, documentId, deviceIdConcat, "intervention");
                scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be a <font color='#00FF00'>CONTROL DAY</font>.",  title, documentId, deviceIdConcat, "intervention");
            }
        }
    }

    private void setupEvery2DaysSchedule(String deviceIdConcat, String documentId, LocalDate startDate, String title, int duration) {
        for (int i = 1; i <= duration; i++)  {
            LocalDate currentDate = startDate.plusDays(i - 1);
            if (i % 4 == 1 || i % 4 == 2) { // Control days
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "control");
                if (i % 4 == 2) {
                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be an <font color='#FF0000'>INTERVENTION DAY</font>. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "control");
                }
            } else { // Intervention days i % 4 == 3 || i % 4 == 0
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "intervention");
                scheduleNotificationForDay("Morning", currentDate, "TODAY is an <font color='#FF0000'>INTERVENTION DAY</font>. Make sure you have your notifications on!", title, documentId, deviceIdConcat, "intervention");
                if (i % 4 == 3) {
//                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be an INTERVENTION DAY. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "intervention");
                } else {
                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be a <font color='#00FF00'>CONTROL DAY</font>.", title, documentId, deviceIdConcat, "intervention");
                }
            }
        }
    }

    private void setupWeeklySchedule(String deviceIdConcat, String documentId, LocalDate startDate, String title, int duration) {
        for (int i = 1; i <= duration; i++) {
            LocalDate currentDate = startDate.plusDays(i - 1);

            if ((i - 1) / 7 % 2 == 0) { // Control week
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "control");
                if ((i - 1) % 7 == 6) { // Last day of control week
                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be an <font color='#FF0000'>INTERVENTION DAY</font>. When you go to sleep, don’t forget to turn on your intervention!", title, documentId, deviceIdConcat, "control");
                }
            } else { // Intervention week
                saveExperimentDay(deviceIdConcat, documentId, currentDate, "intervention");
                if ((i - 1) % 7 == 0) { // First day of intervention week
                    scheduleNotificationForDay("Morning", currentDate, "TODAY is an <font color='#FF0000'>INTERVENTION DAY</font>. Make sure you have your notifications on!", title, documentId, deviceIdConcat, "intervention");
                } else if ((i - 1) % 7 == 6) { // Last day of intervention week
                    scheduleNotificationForDay("Evening", currentDate, "TOMORROW will be a <font color='#00FF00'>CONTROL DAY</font>.", title, documentId, deviceIdConcat, "intervention");
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
            notificationTime.set(Calendar.HOUR_OF_DAY, 20); // 8 PM
            notificationTime.set(Calendar.MINUTE, 0);
        }

        long initialDelay = notificationTime.getTimeInMillis() - now.getTimeInMillis();
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(1); // Schedule for the next day if the time has already passed today
        }


        //test notification
//        OneTimeWorkRequest testWorkRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
//                .setInputData(data)
//                .addTag("TestNotification")
//                .build();
//
//        WorkManager.getInstance(getApplicationContext()).enqueue(testWorkRequest);


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
                        String duration = lastExperiment.getString("duration");
//                        Boolean isRunning = lastExperiment.getBoolean("isRunning");

                        // Example of setting values to UI elements (make sure this runs on the UI thread if it's not already)
                        runOnUiThread(() -> {
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) scheduleSpinner.getAdapter();
                            int position = adapter.getPosition(schedule); // Get the position of the item in the adapter

                            ArrayAdapter<CharSequence> adapter2 = (ArrayAdapter<CharSequence>) durationSpinner.getAdapter();
                            int durPosition = adapter2.getPosition(duration); // Get the position of the item in the adapter

                            experimentTitleInput.setText(title);
                            experimentGoalInput.setText(goal);
                            stepsTakenInput.setText(steps);
                            scheduleSpinner.setSelection(position);
                            durationSpinner.setSelection(durPosition);
//                            runningSwitch.setChecked(isRunning != null && isRunning);
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

    private int getIntervalFromDuration(String duration) {
        switch (duration) {
            case "2 Weeks":
                return 14;
            case "4 Weeks":
                return 28;
            case "6 Weeks":
                return 42;
            case "8 Weeks":
                return 56;
            default:
                return 28; // Default to 4 weeks if the duration is not recognized
        }
    }

    private void fetchAndDisableLastExperiment(Runnable onComplete) {
        String deviceIdConcat = deviceId + "-" + DeviceModel;

        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments")
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot lastExperiment = task.getResult().getDocuments().get(0);
                        String lastExperimentId = lastExperiment.getId();
                        Boolean isRunning = lastExperiment.getBoolean("isRunning");

                        if (isRunning != null && isRunning) {
                            FireStoreDB.collection("Devices").document(deviceIdConcat)
                                    .collection("experiments").document(lastExperimentId)
                                    .update("isRunning", false)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Log.d("Firestore", "Last experiment's running flag set to false.");
                                        } else {
                                            Log.e("Firestore", "Failed to update last experiment's running flag.");
                                        }
                                        onComplete.run();
                                    });
                        } else {
                            onComplete.run();
                        }
                    } else {
                        onComplete.run();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logout_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
