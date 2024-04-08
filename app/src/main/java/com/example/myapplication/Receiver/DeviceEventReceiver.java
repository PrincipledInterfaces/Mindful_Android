package com.example.myapplication.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.example.myapplication.Model.Session;

public class DeviceEventReceiver extends BroadcastReceiver {
    private static final String FILE_NAME = "DeviceEvent.json";
    private FirebaseFirestore firestoreDB;
    private String deviceId;

    public DeviceEventReceiver() {
        firestoreDB = FirebaseFirestore.getInstance();
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        // Check if the Intent's action is not null
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    // Screen turned on, save the current timestamp
                    setScreenOnFlag(context, true);
                    saveEvent(context, "Screen On", System.currentTimeMillis());
                    break;
                case Intent.ACTION_USER_PRESENT:
                    // Device unlocked, calculate duration since screen was turned on
                    setScreenOnFlag(context, false);
                    handleUserPresent(context);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    // Screen turned off without unlocking, calculate duration if applicable
                    handleScreenOff(context);
                    break;
            }
        }
    }


    private void saveEvent(Context context, String eventType, long eventTime) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = dateFormat.format(new Date(eventTime));
        String time = timeFormat.format(new Date(eventTime));

        try {
            JSONObject root = new JSONObject();
            String jsonData = loadDataFromFile(context);
            if (!jsonData.isEmpty()) {
                root = new JSONObject(jsonData);
            }

            JSONArray eventsForDate;
            if (!root.has(date)) {
                eventsForDate = new JSONArray();
                root.put(date, eventsForDate);
            } else {
                eventsForDate = root.getJSONArray(date);
            }

            JSONObject eventDetails = new JSONObject();
            eventDetails.put("Time", time);

            JSONObject event = new JSONObject();
            event.put("EventType", eventType);
            eventDetails.put("Event", event);

            eventsForDate.put(eventDetails);

            saveDataToFile(context, root.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void handleUserPresent(Context context) {

        long unlockTime = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = dateFormat.format(new Date(unlockTime));
        String time = timeFormat.format(new Date(unlockTime));

        try {
            // Load existing data or initialize a new structure
            JSONObject root;
            String jsonData = loadDataFromFile(context);
            if (!jsonData.isEmpty()) {
                root = new JSONObject(jsonData);
            } else {
                root = new JSONObject();
            }

            JSONArray dateEvents;
            if (root.has(date)) {
                dateEvents = root.getJSONArray(date);
            } else {
                dateEvents = new JSONArray();
                root.put(date, dateEvents);
            }

            JSONObject eventDetails = new JSONObject();
            eventDetails.put("Time", time);

            JSONObject event = new JSONObject();
            event.put("EventType", "Device Unlocked");
//            event.put("UnlockTime", unlockTime);

            eventDetails.put("Event", event);
            dateEvents.put(eventDetails);

            // Save the updated JSON structure
            saveDataToFile(context, root.toString());
//            Log.e("DeviceEventReceiver", root.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleScreenOff(Context context) {
        long screenOffTime = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = dateFormat.format(new Date(screenOffTime));
        String time = timeFormat.format(new Date(screenOffTime));

        // Determine the event type based on the screen state
        String eventType = getScreenOnFlag(context) ? "Screen Off Without Unlock" : "Device Locked";

        // Always reset the screen on flag after handling the screen off event
        setScreenOnFlag(context, false);

        try {
            JSONObject root;
            String jsonData = loadDataFromFile(context);
            if (!jsonData.isEmpty()) {
                root = new JSONObject(jsonData);
            } else {
                root = new JSONObject();
            }

            JSONArray dateEvents;
            if (root.has(date)) {
                dateEvents = root.getJSONArray(date);
            } else {
                dateEvents = new JSONArray();
                root.put(date, dateEvents);
            }

            JSONObject eventDetails = new JSONObject();
            eventDetails.put("Time", time);

            JSONObject event = new JSONObject();
            event.put("EventType", eventType);

            if (eventType.equals("Device Locked")) {
//                event.put("LockTime", screenOffTime);
                JSONObject appUsageObj = new JSONObject();
                appUsageObj.put("app_name", "App Name");
                appUsageObj.put("foreground_time", "ForegorundTime");
                event.put("AppUsage", appUsageObj);

            }
            eventDetails.put("Event", event);

            dateEvents.put(eventDetails);

            // Save the updated JSON structure back to the file
            saveDataToFile(context, root.toString());
            // uploading data session wise with start and end time
            uploadDataToFirestore(context, processEventsToSessions(dateEvents));
            // uploading in events array
            uploadDataToFirestore(context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void saveDataToFile(Context context, String data) {
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            Log.e("DeviceEventReceiver", "File not found", e);
        } catch (IOException e) {
            Log.e("DeviceEventReceiver", "Error writing to file", e);
        }
    }

    public static String loadDataFromFile(Context context) {
        StringBuilder jsonData = new StringBuilder();
        try (FileInputStream fis = context.openFileInput(FILE_NAME)) {
            int character;
            while ((character = fis.read()) != -1) {
                jsonData.append((char) character);
            }
        } catch (FileNotFoundException e) {
            Log.e("DeviceEventReceiver", "File not found", e);
        } catch (IOException e) {
            Log.e("DeviceEventReceiver", "Error reading from file", e);
        }
        return jsonData.toString();
    }

    private void setScreenOnFlag(Context context, boolean screenOn) {
        SharedPreferences prefs = context.getSharedPreferences("DeviceEventPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("ScreenOnFlag", screenOn).apply();
    }

    private boolean getScreenOnFlag(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("DeviceEventPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("ScreenOnFlag", false);
    }

    private void uploadDataToFirestore(Context context, List<Session> sessions) {

        for (Session session : sessions) {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("StartTime", session.getStartTime());
            sessionData.put("EndTime", session.getEndTime());
            sessionData.put("Events", session.getEvents());
            sessionData.put("Duration(s)", session.getDuration());

//            String documentId = nowInUtc.toLocalDate() + "_" + session.getStartTime().replaceAll("[^a-zA-Z0-9]", "_");
            String documentId = Long.toString(System.currentTimeMillis() / 1000);
            String documentPath = "Devices/" + deviceId + "/Sessions/" + documentId;
            firestoreDB.document(documentPath)
                    .set(sessionData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Successfully uploaded session data for " + documentId))
                    .addOnFailureListener(e -> Log.w("Firestore", "Error uploading session data for " + documentId, e));

        }
    }
private void uploadDataToFirestore(Context context) {
//    deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    ZonedDateTime nowInUtc = ZonedDateTime.now(ZoneId.of("UTC"));
    String todayDate = nowInUtc.toLocalDate().toString();

    String jsonData = loadDataFromFile(context);
    if (!jsonData.isEmpty()) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            if (!jsonObject.has(todayDate)) {
                Log.d("FirestoreUpload", "No events for today to upload.");
                return; // Exit if there are no events for today.
            }

            JSONArray todayEvents = jsonObject.getJSONArray(todayDate);
            List<Map<String, Object>> eventsList = new ArrayList<>();
            for (int i = 0; i < todayEvents.length(); i++) {
                JSONObject eventObj = todayEvents.getJSONObject(i);
                Map<String, Object> event = new HashMap<>();
                event.put("Time", eventObj.getString("Time"));

                JSONObject eventDetails = eventObj.getJSONObject("Event");
                Map<String, Object> eventDetailsMap = new HashMap<>();
                eventDetailsMap.put("EventType", eventDetails.getString("EventType"));

                // Include additional fields as necessary
                if(eventDetails.has("UnlockTime")) {
                    eventDetailsMap.put("UnlockTime", eventDetails.getLong("UnlockTime"));
                }

                event.put("Event", eventDetailsMap);
                eventsList.add(event);
            }

            // Upload today's events
            firestoreDB.collection("Devices").document(deviceId)
                    .collection("Events").document(todayDate)
                    .set(Map.of("Events", eventsList))
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Successfully uploaded today's events."))
                    .addOnFailureListener(e -> Log.w("Firestore", "Error uploading document", e));

        } catch (JSONException e) {
            Log.e("DeviceEventReceiver", "JSON parsing error", e);
        }
    }
}




    private List<Session> processEventsToSessions(JSONArray eventsArray) throws JSONException {
        List<Session> sessions = new ArrayList<>();
        Session currentSession = null;

        for (int i = 0; i < eventsArray.length(); i++) {
            JSONObject eventObj = eventsArray.getJSONObject(i);
            String eventType = eventObj.getJSONObject("Event").optString("EventType");
            String eventTime = eventObj.optString("Time");

            if ("Screen On".equals(eventType) && currentSession == null) {
                currentSession = new Session(eventTime);
            }

            if (currentSession != null) {
                // Add the event to the current session
                Map<String, Object> eventDetails = new HashMap<>();
                eventDetails.put("Time", eventTime);
                eventDetails.put("EventType", eventType);
                if (eventObj.getJSONObject("Event").has("UnlockTime")) {
                    long unlockTime = eventObj.getJSONObject("Event").optLong("UnlockTime");
                    eventDetails.put("UnlockTime", unlockTime);

                } else if (eventObj.getJSONObject("Event").has("LockTime")) {
                    long lockTime = eventObj.getJSONObject("Event").optLong("LockTime");
                    eventDetails.put("LockTime", lockTime);
                }
//                else if (eventObj.getJSONObject("Event").has("AppUsage")) {
//                    JSONObject appUsageObj = eventObj.getJSONObject("Event").getJSONObject("AppUsage");
//                    eventDetails.put("AppUsage", appUsageObj);
//                }
                currentSession.getEvents().add(eventDetails);
            }

            if (("Device Locked".equals(eventType) || "Screen Off Without Unlock".equals(eventType)) && currentSession != null) {
                currentSession.setEndTime(eventTime);
                sessions.add(currentSession);
                currentSession = null; // End the current session
            }
        }

        // Handle cases where the last session doesn't end with a locking event
        if (currentSession != null) {
            sessions.add(currentSession);
        }

        return sessions;
    }



}
