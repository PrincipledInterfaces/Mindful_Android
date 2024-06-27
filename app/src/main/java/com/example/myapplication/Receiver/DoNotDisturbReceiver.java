package com.example.myapplication.Receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

public class DoNotDisturbReceiver extends BroadcastReceiver {
    public static final String ACTION_ENABLE_DND = "com.example.myapplication.ACTION_ENABLE_DND";
    public static final String ACTION_DISABLE_DND = "com.example.myapplication.ACTION_DISABLE_DND";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_ENABLE_DND.equals(action)) {
                setDoNotDisturbMode(context, true);
            } else if (ACTION_DISABLE_DND.equals(action)) {
                setDoNotDisturbMode(context, false);
            }
        }
    }

    private void setDoNotDisturbMode(Context context, boolean enable) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    int mode = enable ? NotificationManager.INTERRUPTION_FILTER_NONE : NotificationManager.INTERRUPTION_FILTER_ALL;
                    notificationManager.setInterruptionFilter(mode);
                    String message = enable ? "Do Not Disturb enabled" : "Do Not Disturb disabled";
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                } else {
                    // Open the notification policy access settings
                    Intent settingsIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(settingsIntent);
                }
            }
        }
    }
}

