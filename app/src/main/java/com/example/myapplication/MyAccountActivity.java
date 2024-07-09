package com.example.myapplication;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.Util.AuthenticationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Objects;

public class MyAccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        MaterialToolbar toolbar = findViewById(R.id.top_app_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("My Account");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Example of setting user email in TextView
        TextView emailTextView = findViewById(R.id.user_email);
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        emailTextView.setText(userEmail);

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

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteUserAccount();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        if (user != null) {
            // Delete user's authentication record
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MyAccountActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
//                    mAuth.signOut();
                    AuthenticationUtils.logoutUser(this);
                    finish();
                } else {
                    Toast.makeText(MyAccountActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
