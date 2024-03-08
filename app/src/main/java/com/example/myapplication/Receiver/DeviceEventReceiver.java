package com.example.myapplication.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.myapplication.Worker.DeviceEventWorker;

public class DeviceEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("DeviceEventPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            // Device unlocked, store the current timestamp
            long unlockTime = System.currentTimeMillis();
            editor.putLong("unlockTime", unlockTime);
            editor.apply();
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            // Device locked, get the unlock timestamp and current timestamp
            long unlockTime = prefs.getLong("unlockTime", 0);
            long lockTime = System.currentTimeMillis();

            if (unlockTime != 0) {
                // Calculate duration the device remained unlocked (optional)
                long duration = lockTime - unlockTime;

                // Prepare data for the Worker
                Data inputData = new Data.Builder()
                        .putLong("unlockTime", unlockTime)
                        .putLong("lockTime", lockTime)
                        .build();

                // Enqueue the WorkRequest
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DeviceEventWorker.class)
                        .setInputData(inputData)
                        .build();

                WorkManager.getInstance(context).enqueue(workRequest);

                // Clear the stored unlockTime
                editor.remove("unlockTime");
                editor.apply();
            }
        }
    }
}

