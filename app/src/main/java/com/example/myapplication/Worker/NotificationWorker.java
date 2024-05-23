package com.example.myapplication.Worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.core.text.HtmlCompat;

import com.example.myapplication.R;

public class NotificationWorker extends Worker {

    public NotificationWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        // Get the experiment details from input data
        String title = getInputData().getString("title");
        String description = getInputData().getString("description");

        // Create a custom notification
        createCustomNotification(title, description);

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }

    private void createCustomNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "experiment_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Experiment Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        CharSequence formattedMessage = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY);
        CharSequence formattedTitle = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(formattedTitle)
                .setContentText(formattedMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(formattedMessage))
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}

