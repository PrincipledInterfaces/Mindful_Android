package com.example.myapplication.Worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.core.text.HtmlCompat;

import com.example.myapplication.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationWorker extends Worker {
    private FirebaseFirestore FireStoreDB;
    public NotificationWorker(Context context, WorkerParameters params) {
        super(context, params);
        FireStoreDB = FirebaseFirestore.getInstance();
    }

    @Override
    public Result doWork() {
        // Get the experiment details from input data
        String title = getInputData().getString("title");
        String description = getInputData().getString("description");

        String deviceIdConcat = getInputData().getString("deviceIdConcat");
        String documentId = getInputData().getString("documentId");
        String dayType = getInputData().getString("dayType");
        String date = getInputData().getString("date");

        // Create a custom notification
        createCustomNotification(title, description);

        updateNotificationFlag(deviceIdConcat, documentId, date, dayType, true);

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

        title = "Experiment: " + title;
        CharSequence formattedMessage = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY);
        CharSequence formattedTitle = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY);

        // Create the custom notification layout

        RemoteViews contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification);
        contentView.setTextViewText(R.id.title, formattedTitle);
        contentView.setTextViewText(R.id.text, formattedMessage);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setCustomContentView(contentView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(true);


//        Log.e("testnotification", formattedMessage.toString());

        notificationManager.notify(1, builder.build());
    }

    private void updateNotificationFlag(String deviceIdConcat, String documentId, String date, String dayType, boolean isFulfilled) {
        // Update the notification flag in Firestore
        FireStoreDB.collection("Devices").document(deviceIdConcat)
                .collection("experiments").document(documentId)
                .collection(dayType + "_days").document("day_" + date)
                .update("notificationsFulfilled", isFulfilled)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Notification flag successfully updated!"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating notification flag", e));
    }
}

