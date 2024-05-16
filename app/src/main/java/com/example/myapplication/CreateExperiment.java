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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

import com.example.myapplication.Util.AuthenticationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CreateExperiment extends AppCompatActivity {
    private EditText experimentTitleInput;
    private EditText experimentGoalInput;
    private EditText stepsTakenInput;

    private Spinner scheduleSpinner;
    private SwitchMaterial runningSwitch;

    private String deviceId;

    String DeviceModel;

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
        fetchLastExperiment();

        // Button initialization and setOnClickListener
        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitExperiment();
            }
        });

    }

    private void submitExperiment() {
        // Retrieving the text from EditText fields
        String title = experimentTitleInput.getText().toString();
        String goal = experimentGoalInput.getText().toString();
        String steps = stepsTakenInput.getText().toString();
        String schedule = scheduleSpinner.getSelectedItem().toString();
        boolean isRunning = runningSwitch.isChecked();

        // Optional: Validate the inputs
        if(title.isEmpty() || goal.isEmpty() || steps.isEmpty()) {
            Toast.makeText(CreateExperiment.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
        } else {
            saveExperimentToFirestore(title, goal, steps, schedule, isRunning);
            Toast.makeText(CreateExperiment.this, "Experiment Submitted Successfully", Toast.LENGTH_SHORT).show();

        }
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

        db.collection("Devices").document(deviceIdConcat)
                .collection("experiments").document(documentId)
                .set(experiment)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Experiment successfully written!"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error writing document", e));



    }


    private void fetchLastExperiment() {
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

                        // Log for debugging
                        Log.d("Firestore", "Last experiment details: " + lastExperiment.getData());

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
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching document", e);
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
