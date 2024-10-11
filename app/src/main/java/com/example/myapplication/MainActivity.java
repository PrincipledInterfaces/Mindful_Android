package com.example.myapplication;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Adapter.DeviceEventAdapter;
import com.example.myapplication.Adapter.MultiSelectAdapter;
import com.example.myapplication.Model.DeviceEvent;
import com.example.myapplication.Model.Survey.MonthlySurvey;
import com.example.myapplication.Model.Survey.QuestionAnswer;
import com.example.myapplication.Model.SurveyDetails;
import com.example.myapplication.Model.UsageStatsModel;
import com.example.myapplication.Service.AppUsageService;
import com.example.myapplication.Service.DeviceEventService;
import com.example.myapplication.Util.AuthenticationUtils;
import com.example.myapplication.Util.FirebaseUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MainActivity extends Activity {
    private static final int CREATE_EXPERIMENT_REQUEST_CODE = 1;
    private static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_RUN_DATE = "first_run_date";
    private static final String LAST_SURVEY_DATE = "last_survey_date";
    private static final String SURVEY_SHOWN = "survey_shown";

    private FirebaseAuth auth;
    private FirebaseFirestore FireStoreDB;
    private String fid;

    CountDownLatch latch = new CountDownLatch(1);

    private String deviceIdConcat;
    private List<DeviceEvent> events;
    private String jsonData;

    private TextView textView;
    private TextView summaryTextView;
    private TextView runningExperimentDetailsTextView;
    private TextView emptyMessageTextView;
    private RecyclerView recyclerView;
    private FloatingActionButton button;
    private boolean shouldShowSurvey = false;
    private boolean shouldShowMonthlySurvey = false;
    private AlertDialog surveyDialog;
    private AlertDialog SurveyMonthlyDialog;
    private TextView appSelection;

    private String[] appNames;
    private boolean[] selectedItems;

    private View loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        if (fid == null) {
            fid = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        initializeComponents();

        textView.setText("Welcome, Participant");
        setupToolbar();
        checkUsageStatsPermission();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long firstRunDate = settings.getLong(FIRST_RUN_DATE, 0);
        long currentTime = System.currentTimeMillis();

        if (firstRunDate == 0) {
            // Store the first run date
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(FIRST_RUN_DATE, currentTime);
            editor.putLong(LAST_SURVEY_DATE, currentTime); // Also set the last survey date to now
            editor.apply();
        }

//        long lastSurveyDate = settings.getLong(LAST_SURVEY_DATE, 0);
//        long daysSinceLastSurvey = TimeUnit.MILLISECONDS.toMinutes(currentTime - lastSurveyDate);
//
//        shouldShowSurvey = !settings.getBoolean(SURVEY_SHOWN, false);
//        shouldShowMonthlySurvey = daysSinceLastSurvey >= 1;

        updateSurveyFlags();

        button.setOnClickListener(v -> {
            openActivity(CreateExperiment.class);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission();
            }
        }

    }

    public void openActivity(Class<?> toActivity){
        Intent intent = new Intent(MainActivity.this, toActivity);
        intent.putExtra("fid", fid);
        startActivity(intent);
//        startActivityForResult(intent, CREATE_EXPERIMENT_REQUEST_CODE);
    }

    private void updateSurveyFlags() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastSurveyDate = settings.getLong(LAST_SURVEY_DATE, 0);
        long currentTime = System.currentTimeMillis();
        long daysSinceLastSurvey = TimeUnit.MILLISECONDS.toDays(currentTime - lastSurveyDate);

        shouldShowSurvey = !settings.getBoolean(SURVEY_SHOWN, false);
        shouldShowMonthlySurvey = daysSinceLastSurvey >= 30;  // Assuming 30 days for a month
    }

    private void populateAppSelection(View surveyLayout) {
        PackageManager pm = getPackageManager();
        Intent socialMediaIntent = new Intent(Intent.ACTION_SEND);
        socialMediaIntent.setType("text/plain");

        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(socialMediaIntent, PackageManager.MATCH_ALL);
        List<String> appNameList = new ArrayList<>();

        for (ResolveInfo resolveInfo : resolveInfoList) {
            ApplicationInfo app = resolveInfo.activityInfo.applicationInfo;
            String appName = pm.getApplicationLabel(app).toString();
            if (!appNameList.contains(appName)) { // Avoid duplicates
                appNameList.add(appName);
            }
        }

        appNames = appNameList.toArray(new String[0]);
        selectedItems = new boolean[appNames.length];

        appSelection = surveyLayout.findViewById(R.id.app_selection);
        appSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMultiSelectDialog();
            }
        });
    }

    private void showMultiSelectDialog() {
        final boolean[] tempSelectedItems = selectedItems.clone();
        new AlertDialog.Builder(this)
                .setTitle("Select Apps")
                .setMultiChoiceItems(appNames, tempSelectedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
//                        selectedItems[indexSelected] = isChecked;
                        tempSelectedItems[indexSelected] = isChecked;
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        System.arraycopy(tempSelectedItems, 0, selectedItems, 0, tempSelectedItems.length);
                        updateSelectedItemsText();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }})
                .setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Clear all selections
//                        for (int i = 0; i < selectedItems.length; i++) {
//                            selectedItems[i] = false;
//                        }
                        for (int i = 0; i < tempSelectedItems.length; i++) {
                            tempSelectedItems[i] = false;
                        }
                        appSelection.setText("");
                    }
                })
                .show();
    }

    private void updateSelectedItemsText() {
        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
        for (int i = 0; i < appNames.length; i++) {
            if (selectedItems[i]) {
                SpannableString spannablePart = new SpannableString(appNames[i]);
                spannablePart.setSpan(new ForegroundColorSpan(Color.GRAY), 0, appNames[i].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (spannableBuilder.length() > 0) {
                    spannableBuilder.append(", ");
                }
                spannableBuilder.append(spannablePart);
            }
        }
        appSelection.setText(spannableBuilder);
    }

    private void saveSurveyDetailsToFirestore(List<QuestionAnswer> qa, List<String> selectedApps, String SurveyType) {

        // Set up the date format to use UTC
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formattedTimestamp = dateFormat.format(new Date());

        long timestamp = System.currentTimeMillis() / 1000L;

        String surveyId = "survey_" + formattedTimestamp;

        if (SurveyType.equals("launch")){
            SurveyDetails surveyDetails = new SurveyDetails(qa, selectedApps, timestamp);
            FireStoreDB.collection("Devices").document(deviceIdConcat)
                    .collection("launch_surveys").document(surveyId)
                    .set(surveyDetails)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Survey details saved successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to save survey details.", Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Error saving survey details", e);
                    });
        } else {
            MonthlySurvey surveyDetails = new MonthlySurvey(qa, timestamp);
            FireStoreDB.collection("Devices").document(deviceIdConcat)
                    .collection("monthly_surveys").document(surveyId)
                    .set(surveyDetails)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Survey details saved successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to save survey details.", Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Error saving survey details", e);
                    });
        }



    }

    private void showSurveyModal() {

        // Inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View surveyLayout = inflater.inflate(R.layout.dialog_survey, null);

        loadingScreen = surveyLayout.findViewById(R.id.loading_screen);


        WebView webView = surveyLayout.findViewById(R.id.agreement_details);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadingScreen.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadingScreen.setVisibility(View.GONE);
            }
        });

        String htmlContent = readHtmlFromFile("agreement_details.html");
        webView.loadData(htmlContent, "text/html", "UTF-8");

        ViewFlipper viewFlipper = surveyLayout.findViewById(R.id.view_flipper);
        ImageButton backButton = surveyLayout.findViewById(R.id.back_button);
        ImageButton forwardButton = surveyLayout.findViewById(R.id.forward_button);
        TextView dialogTitle = surveyLayout.findViewById(R.id.dialog_title);
        MaterialButton agreeButton = surveyLayout.findViewById(R.id.agree_button);
        MaterialButton submitButton = surveyLayout.findViewById(R.id.submit_button);

        submitButton.setEnabled(false); // Disable submit button until agreement is accepted

        // Populate the app selection
        populateAppSelection(surveyLayout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(surveyLayout)
                .setCancelable(false);

        surveyDialog = builder.create();

        backButton.setOnClickListener(v -> {
            viewFlipper.showPrevious();
            dialogTitle.setText("Agreement Terms");
            backButton.setVisibility(View.GONE);
            forwardButton.setVisibility(View.VISIBLE);
        });

        forwardButton.setOnClickListener(v -> {
            viewFlipper.showNext();
            dialogTitle.setText("Survey");
            backButton.setVisibility(View.VISIBLE);
            forwardButton.setVisibility(View.GONE);
        });

        agreeButton.setOnClickListener(v -> {
            agreeButton.setEnabled(false);
            submitButton.setEnabled(true);  // Enable the submit button when user agrees
            forwardButton.performClick();
        });

        submitButton.setOnClickListener(v -> {

            // Handle survey submission here
            TextView question1 = surveyLayout.findViewById(R.id.question1);
            TextView question2 = surveyLayout.findViewById(R.id.question2);
            TextView likertQuestion = surveyLayout.findViewById(R.id.likert_question);

            // Retrieve answer TextInputEditText elements
            TextInputEditText answer1 = surveyLayout.findViewById(R.id.answer1);
            TextInputEditText answer2 = surveyLayout.findViewById(R.id.answer2);

            SeekBar likertSeekBar = surveyLayout.findViewById(R.id.likert_seekbar);

            // Get the text from the views
            String question1Text = question1.getText().toString();
            String answer1Text = answer1.getText().toString();
            String question2Text = question2.getText().toString();
            String answer2Text = answer2.getText().toString();

            int seekBarValue = likertSeekBar.getProgress() + 1;

            // Create a list of QuestionAnswer objects
            List<QuestionAnswer> questionsAndAnswers = new ArrayList<>();
            questionsAndAnswers.add(new QuestionAnswer(question1Text, answer1Text));
            questionsAndAnswers.add(new QuestionAnswer(question2Text, answer2Text));
            questionsAndAnswers.add(new QuestionAnswer(likertQuestion.getText().toString(), String.valueOf(seekBarValue)));

            // Collect selected apps
            List<String> selectedApps = new ArrayList<>();
            for (int i = 0; i < appNames.length; i++) {
                if (selectedItems[i]) {
                    selectedApps.add(appNames[i]);
                }
            }

            saveSurveyDetailsToFirestore(questionsAndAnswers, selectedApps, "launch");

            // Mark survey as shown
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SURVEY_SHOWN, true);
            editor.apply();

            shouldShowSurvey = false; // Update the flag


            surveyDialog.dismiss();
            openActivity(PredefinedExperimentSelectionActivity.class);

        });

        surveyDialog.show();
    }

    private String readHtmlFromFile(String fileName) {
        StringBuilder htmlString = new StringBuilder();
        try {
            InputStream inputStream = getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                htmlString.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return htmlString.toString();
    }

    private void showMonthlySurveyModal() {
        LayoutInflater inflater = getLayoutInflater();
        View surveyLayout = inflater.inflate(R.layout.dialog_monthly_survey, null);

        TextView question1 = surveyLayout.findViewById(R.id.question1);
        TextView question2 = surveyLayout.findViewById(R.id.question2);
        TextView question3 = surveyLayout.findViewById(R.id.question3);
        TextInputEditText answer1 = surveyLayout.findViewById(R.id.answer1);
        TextInputEditText answer2 = surveyLayout.findViewById(R.id.answer2);
        TextView answer3 = surveyLayout.findViewById(R.id.answer3);

        // Find CheckBoxes
        TextView checkboxQuestion = surveyLayout.findViewById(R.id.checkbox_question);
        CheckBox checkboxFocusMode = surveyLayout.findViewById(R.id.checkbox_focus_mode);
        CheckBox checkboxGrayscaleScreen = surveyLayout.findViewById(R.id.checkbox_grayscale_screen);
        CheckBox checkboxNotificationsOff = surveyLayout.findViewById(R.id.checkbox_notifications_off);
        CheckBox checkboxDeletedApps = surveyLayout.findViewById(R.id.checkbox_deleted_apps);

        MaterialButton submitButton = surveyLayout.findViewById(R.id.submit_button);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(surveyLayout)
                .setCancelable(false);

        SurveyMonthlyDialog = builder.create();

        submitButton.setOnClickListener(v -> {
            String question1Text = question1.getText().toString();
            String answer1Text = answer1.getText().toString();
            String question2Text = question2.getText().toString();
            String answer2Text = answer2.getText().toString();
            String question3Text = question3.getText().toString();
            String answer3Text = answer3.getText().toString();

            // Get checkbox states
            String checkboxQuestionText = checkboxQuestion.getText().toString();
            boolean isFocusModeChecked = checkboxFocusMode.isChecked();
            boolean isGrayscaleScreenChecked = checkboxGrayscaleScreen.isChecked();
            boolean areNotificationsOffChecked = checkboxNotificationsOff.isChecked();
            boolean areDeletedAppsChecked = checkboxDeletedApps.isChecked();

            List<QuestionAnswer> checkboxQuestionAndAnswers = new ArrayList<>();
            checkboxQuestionAndAnswers.add(new QuestionAnswer("Focus Mode: ", String.valueOf(isFocusModeChecked)));
            checkboxQuestionAndAnswers.add(new QuestionAnswer("Grayscale screen: ", String.valueOf(isGrayscaleScreenChecked)));
            checkboxQuestionAndAnswers.add(new QuestionAnswer("Notifications off: ", String.valueOf(areNotificationsOffChecked)));
            checkboxQuestionAndAnswers.add(new QuestionAnswer("Deleted apps: ", String.valueOf(areDeletedAppsChecked)));

            List<QuestionAnswer> questionsAndAnswers = new ArrayList<>();
            questionsAndAnswers.add(new QuestionAnswer(question1Text, answer1Text));
            questionsAndAnswers.add(new QuestionAnswer(question2Text, answer2Text));
            questionsAndAnswers.add(new QuestionAnswer(question3Text, answer3Text));
            questionsAndAnswers.add(new QuestionAnswer(checkboxQuestionText, checkboxQuestionAndAnswers));

            // Save survey details to Firestore
            saveSurveyDetailsToFirestore(questionsAndAnswers, null, "monthly");

            // Update the last survey date
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(LAST_SURVEY_DATE, System.currentTimeMillis());
            editor.apply();

            SurveyMonthlyDialog.dismiss();
        });

        SurveyMonthlyDialog.show();
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        updateRunningExperimentDetails();
//        if (shouldShowSurvey && (surveyDialog == null || !surveyDialog.isShowing())) {
//            showSurveyModal();
//        }
//
//        if (shouldShowMonthlySurvey && (SurveyMonthlyDialog == null || !SurveyMonthlyDialog.isShowing())) {
//            showMonthlySurveyModal();
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRunningExperimentDetails();

        updateSurveyFlags();

        if (shouldShowSurvey && (surveyDialog == null || !surveyDialog.isShowing())) {
            showSurveyModal();
        }

        if (shouldShowMonthlySurvey && (SurveyMonthlyDialog == null || !SurveyMonthlyDialog.isShowing())) {
            showMonthlySurveyModal();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (surveyDialog != null && surveyDialog.isShowing()) {
            surveyDialog.dismiss();
        }
        if(SurveyMonthlyDialog !=  null && SurveyMonthlyDialog.isShowing()){
            SurveyMonthlyDialog.dismiss();
        }
    }

    private void initializeComponents() {
        FireStoreDB = FirebaseFirestore.getInstance();
        deviceIdConcat = fid + "-" + Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
        runningExperimentDetailsTextView = findViewById(R.id.running_experiment_details);
        summaryTextView = findViewById(R.id.summaryTextView);
        textView = findViewById(R.id.user_details);
        emptyMessageTextView = findViewById(R.id.empty_message);
        button = findViewById(R.id.new_experiment);
        recyclerView = findViewById(R.id.recyclerView);

//        loadingScreen = findViewById(R.id.loading_screen);
    }
    private void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_toolbar);
        toolbar.setTitle("Dashboard");
        toolbar.setNavigationIcon(null);
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
            if (events.isEmpty()) {
                emptyMessageTextView.setVisibility(View.VISIBLE); // Show empty message
                recyclerView.setVisibility(View.GONE); // Hide RecyclerView
            } else {
                emptyMessageTextView.setVisibility(View.GONE); // Hide empty message
                recyclerView.setVisibility(View.VISIBLE); // Show RecyclerView
                displayEvents(events); // Display events
            }
        } catch (Exception e) {
            Log.e("Reading Json File", "An unexpected error occurred", e);
            emptyMessageTextView.setVisibility(View.VISIBLE); // Show empty message on error
            recyclerView.setVisibility(View.GONE); // Hide RecyclerView
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

    private List<DeviceEvent> parseDeviceEvents(String jsonData) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String todayDate = dateFormat.format(new Date());

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray todayEvents = jsonObject.getJSONArray(todayDate);
            List<DeviceEvent> eventsList = new ArrayList<>();

            for (int i = 0; i < todayEvents.length(); i++) {
                JSONObject eventObj = todayEvents.getJSONObject(i);
                String eventType = eventObj.getString("EventType");

                DeviceEvent event = new DeviceEvent(eventType, eventObj.getLong("Time"), 0);
                eventsList.add(event);

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
        if (item.getItemId() == R.id.action_refresh) {
            updateRunningExperimentDetails();
            fetchAndDisplayEvents();
            Toast.makeText(MainActivity.this, "Data refreshed", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (item.getItemId() == R.id.my_account) {
            Intent intent = new Intent(this, MyAccountActivity.class);
            intent.putExtra("deviceIdConcat", deviceIdConcat);
            startActivity(intent);
            return true;
        }
        else if (item.getItemId() == R.id.help){
            Intent intent = new Intent(this, PredefinedExperimentSelectionActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
            new AlertDialog.Builder(this)
                    .setTitle("Notification Permission Needed")
                    .setMessage("This app needs the Notification permission to send you notifications.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
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
