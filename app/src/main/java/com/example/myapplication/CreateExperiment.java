package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
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
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

public class CreateExperiment extends AppCompatActivity {
    private EditText startDateInput;
    private EditText endDateInput;

    private EditText startTimeInput;
    private EditText endTimeInput;

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

        // Set up the date inputs with TextWatchers and validation
        setupDateInputs();
    }

    private void setupDateInputs() {
        startDateInput = findViewById(R.id.start_date_input);
        endDateInput = findViewById(R.id.end_date_input);
        startTimeInput = findViewById(R.id.start_time_input);
        endTimeInput = findViewById(R.id.end_time_input);

        setupDateInputFormat(startDateInput);
        setupDateInputFormat(endDateInput);
        setupTimeInputFormat(startTimeInput);
        setupTimeInputFormat(endTimeInput);
    }


    private void setupDateInputFormat(EditText dateInput) {
        dateInput.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private boolean isDeleting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = count > after;  // Determine if the change was a deletion
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String userInput = s.toString().replaceAll("[^\\d]", "");  // Remove non-digits
                if (!userInput.equals(current)) {
                    if (userInput.length() <= 8) {  // Ensure input length does not exceed 8 digits for DDMMYYYY
                        String formatted = formatDateString(userInput);
                        current = formatted;

                        dateInput.removeTextChangedListener(this);  // Remove listener
                        dateInput.setText(formatted);
                        dateInput.setSelection(formatted.length());
                        dateInput.addTextChangedListener(this);  // Reattach listener
                    } else {
                        dateInput.removeTextChangedListener(this);  // Remove listener
                        dateInput.setText(current);  // Reset to last valid format
                        dateInput.setSelection(current.length());
                        dateInput.addTextChangedListener(this);  // Reattach listener
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            private String formatDateString(String input) {
                StringBuilder formatted = new StringBuilder(input);

                // Only add slashes when the input length and positions are appropriate, and not deleting
                if (formatted.length() > 4) {
                    formatted.insert(2, '/');
                    formatted.insert(5, '/');
                } else if (formatted.length() > 2) {
                    formatted.insert(2, '/');
                }

                return formatted.toString();
            }
        });
    }

    private void setupTimeInputFormat(EditText timeInput) {
        timeInput.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private boolean isDeleting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = count > after;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String userInput = s.toString().replaceAll("[^\\d]", "");  // Keep only digits
                if (!userInput.equals(current)) {
                    if (userInput.length() <= 4) {  // HHmm
                        String formatted = formatTimeString(userInput);
                        current = formatted;

                        timeInput.removeTextChangedListener(this);
                        timeInput.setText(formatted);
                        timeInput.setSelection(formatted.length());
                        timeInput.addTextChangedListener(this);
                    } else {
                        timeInput.removeTextChangedListener(this);
                        timeInput.setText(current);
                        timeInput.setSelection(current.length());
                        timeInput.addTextChangedListener(this);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            private String formatTimeString(String input) {
                StringBuilder formatted = new StringBuilder(input);

                // Insert colon for time format HH:mm
                if (formatted.length() > 2) {
                    formatted.insert(2, ':');
                }

                return formatted.toString();
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
        int id = item.getItemId();
        if (id == R.id.menu_logout) {
            AuthenticationUtils.logoutUser(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
