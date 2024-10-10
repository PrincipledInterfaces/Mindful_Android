package com.example.myapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.Util.AuthenticationUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyAccountActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser user;
    private String deviceIdConcat;
    private View loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        db = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        deviceIdConcat = intent.getStringExtra("deviceIdConcat");

        loadingScreen = findViewById(R.id.loading_screen);

        MaterialToolbar toolbar = findViewById(R.id.top_app_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("My Account");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find the delete account button
        MaterialButton deleteAccountButton = findViewById(R.id.button_delete_account);

        // Set an OnClickListener for the delete account button
        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteAccountDialog();
            }
        });
    }

//    private void showDeleteAccountDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Delete Account")
//                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
//                .setPositiveButton("Delete", null)
//                .setNegativeButton("Cancel", null);
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//
//        // Get the "Delete" button
//        Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
//
//        // Apply padding programmatically
//        int paddingHorizontal = (int) (16 * getResources().getDisplayMetrics().density); // Convert 16dp to pixels
//        int paddingVertical = (int) (8 * getResources().getDisplayMetrics().density); // Convert 8dp to pixels
//        deleteButton.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
//
//        // Set the background color programmatically
//        deleteButton.setBackgroundColor(ContextCompat.getColor(this, R.color.custom_red));
//        deleteButton.setTextColor(Color.WHITE);
//
//        // Set the onClickListener after customization
//        deleteButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showLoadingScreen();
//                deleteUserAccount();
//                dialog.dismiss();
//            }
//        });
//    }

    private void showDeleteAccountDialog() {
        // Use MaterialAlertDialogBuilder instead of AlertDialog.Builder
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

        builder.setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", null)  // We'll customize this button after the dialog is shown
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Get the "Delete" button after the dialog is shown
        Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        // Apply padding programmatically
        int paddingHorizontal = (int) (16 * getResources().getDisplayMetrics().density); // Convert 16dp to pixels
        int paddingVertical = (int) (8 * getResources().getDisplayMetrics().density); // Convert 8dp to pixels
        deleteButton.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        // Set the background color programmatically
        deleteButton.setBackgroundColor(ContextCompat.getColor(this, R.color.custom_red));  // Your custom color
        deleteButton.setTextColor(Color.WHITE);  // Set text color to white

        // Set the onClickListener after customization
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingScreen();
                deleteUserAccount();
                dialog.dismiss();
            }
        });
    }

    private void showLoadingScreen() {
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
    }

    private void deleteUserAccount() {
        Log.d("MyAccountActivity", "Attempting to delete document with ID: " + deviceIdConcat);
        if (deviceIdConcat != null && !deviceIdConcat.isEmpty()) {
            DocumentReference docRef = db.collection("Devices").document(deviceIdConcat);
            deleteDocumentWithSubcollections(docRef)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("MyAccountActivity", "Document and its subcollections successfully deleted!");
                            hideLoadingScreen();
                            // Handle successful deletion
                            AlertDialog.Builder builder = new AlertDialog.Builder(MyAccountActivity.this);
                            builder.setTitle("Success")
                                    .setMessage("Document and its subcollections successfully deleted.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            clearAppDataAndUninstall();
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    clearAppDataAndUninstall();
                                }
                            });
                            dialog.show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            hideLoadingScreen();
                            Log.e("MyAccountActivity", "Error deleting document and its subcollections", e);
                            // Create and show failure dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(MyAccountActivity.this);
                            builder.setTitle("Error")
                                    .setMessage("Error deleting document and its subcollections: " + e.getMessage())
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Continue with whatever needs to be done
                                        }
                                    })
                                    .show();
                        }
                    });
        } else {
            hideLoadingScreen();
            Log.e("MyAccountActivity", "Device ID is null or empty");
            AlertDialog.Builder builder = new AlertDialog.Builder(MyAccountActivity.this);
            builder.setTitle("Error")
                    .setMessage("Device ID is invalid. Cannot delete document.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private Task<Void> deleteDocumentWithSubcollections(DocumentReference docRef) {
        Log.e("sparta", docRef.getPath());
        // Create a list of all tasks for deleting subcollections and documents
        List<Task<Void>> tasks = new ArrayList<>();

        // Add tasks to delete subcollections "Events" and "experiments"
        tasks.add(deleteSubcollection(docRef.collection("Events"), false));
        tasks.add(deleteSubcollection(docRef.collection("experiments"), true));

        tasks.add(deleteSubcollection(docRef.collection("intervention_days"), true));
        tasks.add(deleteSubcollection(docRef.collection("control_days"), true));

//        tasks.add(deleteExperimentsSubcollections(docRef.collection("experiments")));

        tasks.add(deleteSubcollection(docRef.collection("launch_surveys"), false));
        tasks.add(deleteSubcollection(docRef.collection("monthly_surveys"), false));

        // Wait for all subcollections to be deleted, then delete the main document
        return Tasks.whenAll(tasks).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return docRef.delete();
        });
    }

    private Task<Void> deleteSubcollection(CollectionReference collection, boolean isExp) {
        return collection.get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    QuerySnapshot snapshots = task.getResult();
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (DocumentSnapshot snapshot : snapshots) {
                        deleteTasks.add(deleteDocumentWithSubcollections(snapshot.getReference()));
                    }
                    return Tasks.whenAll(deleteTasks);
                });
    }

    private void clearAppDataAndUninstall() {
        // Clear app data
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.clearApplicationUserData();

        // Close the application
        finishAffinity();
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
