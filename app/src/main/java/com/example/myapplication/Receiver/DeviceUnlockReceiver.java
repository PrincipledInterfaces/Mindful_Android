package com.example.myapplication.Receiver;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;

import com.example.myapplication.Service.AppUsageService;

public class DeviceUnlockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            // Device was unlocked, handle app usage statistics here or start a service to do so
            // For example, start a service to fetch and store app usage statistics
            Intent serviceIntent = new Intent(context, AppUsageService.class);
            context.startService(serviceIntent);
        }
    }
}
