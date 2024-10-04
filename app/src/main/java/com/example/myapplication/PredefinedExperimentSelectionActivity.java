package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Objects;

public class PredefinedExperimentSelectionActivity extends AppCompatActivity {

    private String fid;
    private Spinner PreDefExp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_predefined_experiment_selection);

        Intent intent = getIntent();
        if (intent != null) {
            fid = intent.getStringExtra("fid");
        }
        else {
            fid = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        MaterialToolbar toolbar = findViewById(R.id.top_app_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Predefined Experiments");



        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Setup UI elements
        TextView instructions = findViewById(R.id.instructions_heading);
        Button SubmitButton = findViewById(R.id.submitButton);
//        Button grayscaleButton = findViewById(R.id.GrayScaleExperimentButton);
        PreDefExp = findViewById(R.id.preDefExpSpinner);

        // Handle predefined experiment selection
        SubmitButton.setOnClickListener(v -> {
            int selectedPosition = PreDefExp.getSelectedItemPosition();

            Intent intentActivitySwitch = new Intent(PredefinedExperimentSelectionActivity.this, CreateExperiment.class);
            intentActivitySwitch.putExtra("fid", fid);
            intentActivitySwitch.putExtra("experiment_id", selectedPosition + 1);
            startActivity(intentActivitySwitch);
        });

        // Adjust for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
