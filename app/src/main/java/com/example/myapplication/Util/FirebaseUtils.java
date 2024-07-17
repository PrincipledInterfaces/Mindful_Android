package com.example.myapplication.Util;

import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.installations.FirebaseInstallations;
import androidx.annotation.NonNull;

public class FirebaseUtils {

    public interface FirebaseIdCallback {
        void onSuccess(String fid);
        void onFailure(Exception e);
    }

    public static void getFirebaseInstallationId(FirebaseIdCallback callback) {
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    // Get the Installation ID
                    String fid = task.getResult();
                    callback.onSuccess(fid);
                } else {
                    callback.onFailure(task.getException());
                }
            }
        });
    }
}

