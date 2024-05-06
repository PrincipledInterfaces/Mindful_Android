package com.example.myapplication.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.myapplication.Login;
import com.google.firebase.auth.FirebaseAuth;

public class AuthenticationUtils {

    public static void logoutUser(Context context) {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Start login activity and clear the task stack
        Intent intent = new Intent(context, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}

