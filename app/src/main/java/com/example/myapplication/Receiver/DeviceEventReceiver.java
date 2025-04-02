package com.example.myapplication.Receiver;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.example.myapplication.Model.UsageStatsModel;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;

import com.example.myapplication.Model.Session;

public class DeviceEventReceiver extends BroadcastReceiver {
    private static final String FILE_NAME = "DeviceEvent.json";
    private static final String PREFS_NAME = "DeviceEventPrefs";
    private static final String LAST_UNLOCK_TIME = "LastUnlockTime";
    private FirebaseFirestore firestoreDB;
    private String deviceId;

    String DeviceModel;

    public DeviceEventReceiver() {
        firestoreDB = FirebaseFirestore.getInstance();
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Executors.newSingleThreadExecutor().execute(() -> {
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        if (DeviceModel ==  null){
            DeviceModel = Build.MANUFACTURER + "-" + Build.MODEL.toLowerCase();
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
    });
    }

    private void saveEvent(Context context, String eventType, long eventTime) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
//        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = dateFormat.format(new Date(eventTime));
//        String time = timeFormat.format(new Date(eventTime));
        long time = eventTime/1000;
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

            eventDetails.put("EventType", eventType);

            eventsForDate.put(eventDetails);

            saveDataToFile(context, root.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void setLastUnlockTime(Context context, long time) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(LAST_UNLOCK_TIME, time).apply();
    }

    private long getLastUnlockTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(LAST_UNLOCK_TIME, 0);
    }

    private void handleUserPresent(Context context) {

        long unlockTime = System.currentTimeMillis();

        setLastUnlockTime(context, unlockTime);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
//        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String date = dateFormat.format(new Date(unlockTime));
//        String time = timeFormat.format(new Date(unlockTime));
        long time = unlockTime/1000;

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

            eventDetails.put("EventType", "Device Unlocked");
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
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
//        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = dateFormat.format(new Date(screenOffTime));
//        String time = timeFormat.format(new Date(screenOffTime));
        long time = screenOffTime /1000;

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
            eventDetails.put("EventType", eventType);



            if (eventType.equals("Device Locked")) {
                long unlockTime = getLastUnlockTime(context);
                if (unlockTime == 0) {
                    Log.e("DeviceEventReceiver", "Unlock time not found");
                    return;
                }

//                Map<String, UsageStatsModel> appUsage = fetchAppUsage(context, unlockTime, screenOffTime);
                Multimap<String, UsageStatsModel> appUsage = fetchAppUsage(context, unlockTime, screenOffTime);

                JSONArray appUsageDetails = new JSONArray();
                
                List<Map<String, Object>> appEvents = new ArrayList<>();
                for (Map.Entry<String, UsageStatsModel> entry : appUsage.entries()) {
                    Map<String, Object> appEvent = new HashMap<>();
                    appEvent.put("EventType", entry.getValue().getAppName());
                    appEvent.put("Time", entry.getValue().getUsageDuration());
                    appEvent.put("Order", entry.getValue().getOrder());
                    appEvents.add(appEvent);
                }

                // Sort app events by 'Order'
                Collections.sort(appEvents, byOrder);

                // Add sorted events to appUsageDetails JSONArray
                for (Map<String, Object> sortedEvent : appEvents) {
                    appUsageDetails.put(new JSONObject(sortedEvent));
                }

                // Add the app usage array to the event details
                eventDetails.put("AppUsage", appUsageDetails);
            }

            dateEvents.put(eventDetails);

            // Save the updated JSON structure back to the file
            saveDataToFile(context, root.toString());
            // uploading data session wise with start and end time
//            uploadDataToFirestore(context, processEventsToSessions(dateEvents));
            // uploading in events array
            uploadDataToFirestore(context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


//    private Multimap<String, UsageStatsModel> fetchAppUsage(Context context, long unlockTime, long lockTime) {
//        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
//        PackageManager packageManager = context.getPackageManager();
//
//        UsageEvents events = usm.queryEvents(unlockTime, lockTime);
////        Map<String, UsageStatsModel> aggregatedUsage = new HashMap<>();
//        Multimap<String, UsageStatsModel> aggregatedUsage = ArrayListMultimap.create();
//
//        Map<String, Long> lastForegroundTime = new HashMap<>();
//        int Order = 0;
//
//        while (events.hasNextEvent()) {
//            UsageEvents.Event event = new UsageEvents.Event();
//            events.getNextEvent(event);
//
//            String PN = event.getPackageName();
//            // Skip "One UI Home"
//            if (PN.equals("com.sec.android.app.launcher")) {
//                continue;
//            }
//
//            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
//                lastForegroundTime.put(event.getPackageName(), Math.max(event.getTimeStamp(), unlockTime));
//            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
//                Long foregroundTime = lastForegroundTime.remove(event.getPackageName());
//                if (foregroundTime != null) {
//                    long timeBgd = Math.min(event.getTimeStamp(), lockTime);
//                    long timeSpent = timeBgd - foregroundTime;
//                    if (timeSpent > 0) {
//                        String packageName = event.getPackageName();
//                        String appName;
//                        Order++;
//                        try {
//                            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
//                            appName = (String) packageManager.getApplicationLabel(applicationInfo);
//                            if (packageName.contains("com.whatsapp.w4b")){
//                                appName = "WhatsApp Business";
//                            }
//                        } catch (PackageManager.NameNotFoundException e) {
//                            appName = packageName; // Fallback to package name if app name not found
//                            continue; // Skip this package
//                        }
//
////                        UsageStatsModel appUsageInfo = aggregatedUsage.getOrDefault(packageName, new UsageStatsModel(appName, 0, Order));
//                            aggregatedUsage.put(packageName, new UsageStatsModel(appName, timeSpent, Order));
//
////                        Log.e("orderCheck", String.valueOf(appUsageInfo.getOrder()));
////                        appUsageInfo.addUsageTime(timeSpent);
////                        aggregatedUsage.put(packageName, appUsageInfo);
//                    }
//                }
//            }
//        }
//
//        return aggregatedUsage;
//    }

    private Multimap<String, UsageStatsModel> fetchAppUsage(Context context, long unlockTime, long lockTime) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        PackageManager packageManager = context.getPackageManager();

        UsageEvents events = usm.queryEvents(unlockTime, lockTime);
        Multimap<String, UsageStatsModel> aggregatedUsage = ArrayListMultimap.create();

        Map<String, Long> lastForegroundTime = new HashMap<>();
        String lastAppName = null;
        int order = 0;
        UsageStatsModel currentModel = null;

        while (events.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            events.getNextEvent(event);

            String packageName = event.getPackageName();
            if (packageName.equals("com.sec.android.app.launcher")) {
                continue;
            }

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundTime.put(event.getPackageName(), Math.max(event.getTimeStamp(), unlockTime));
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                Long foregroundTime = lastForegroundTime.remove(event.getPackageName());
                if (foregroundTime != null) {
                    long timeBgd = Math.min(event.getTimeStamp(), lockTime);
                    long timeSpent = timeBgd - foregroundTime;
                    if (timeSpent > 0) {
                        String appName;
                        try {
                            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                            appName = (String) packageManager.getApplicationLabel(applicationInfo);
                            if (packageName.contains("com.whatsapp.w4b")) {
                                appName = "WhatsApp Business";
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            appName = packageName;
                            continue;
                        }

                        if (appName.equals(lastAppName)) {
                            // Add usage time to the existing model
                            if (currentModel != null) {
                                currentModel.addUsageTime(timeSpent);
                            }
                        } else {
                            // Create a new model for the different app
                            order++;
                            currentModel = new UsageStatsModel(appName, timeSpent, order);
                            aggregatedUsage.put(packageName, currentModel);
                            lastAppName = appName;
                        }
                    }
                }
            }
        }

        return aggregatedUsage;
    }



    private String getFileNameForDate(String date) {
        return "DeviceEvent_" + date + ".json";
    }

    private void saveDataToFile(Context context, String date, String data) {
        try (FileOutputStream fos = context.openFileOutput(getFileNameForDate(date), Context.MODE_PRIVATE)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e("DeviceEventReceiver", "Error writing to file", e);
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


    private static final Comparator<Map<String, Object>> byOrder = new Comparator<Map<String, Object>>() {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            return Integer.compare((int) o1.get("Order"), (int) o2.get("Order"));
        }
    };

private void uploadDataToFirestore(Context context) {
//    deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

    String deviceIdConcat = deviceId + "-" + DeviceModel;
//    ZonedDateTime nowInUtc = ZonedDateTime.now(ZoneId.of("UTC"));
//    String todayDate = nowInUtc.toLocalDate().toString();

    LocalDate today = LocalDate.now();
    String todayDate = today.toString();

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
//                String eventType = eventObj.getJSONObject("Event").optString("EventType");

                String eventType = eventObj.getString("EventType");
                Map<String, Object> event = new HashMap<>();
                event.put("Time", eventObj.getString("Time"));
                event.put("EventType", eventType);

                if (eventObj.has("AppUsage")) {
                    JSONArray appUsageArray = eventObj.getJSONArray("AppUsage");
                    for (int j = 0; j < appUsageArray.length(); j++) {
                        JSONObject appUsageEvent = appUsageArray.getJSONObject(j);
//                        JSONObject appDetails = appUsageEvent.getJSONObject("Event");

                        Map<String, Object> appEvent = new HashMap<>();
                        appEvent.put("EventType", appUsageEvent.getString("EventType"));  // "EventType" now contains the app name
                        appEvent.put("Duration", appUsageEvent.getLong("Time"));  // "Time" now contains the usage duration
                        appEvent.put("Order", appUsageEvent.getInt("Order"));
                        eventsList.add(appEvent);  // Treat each app usage as an independent event
                    }
                    eventsList.add(event);  // Add the original event as well
                }
                else {
                    eventsList.add(event); // Add regular events that aren't app usages
                }
            }


            // Upload today's events
            firestoreDB.collection("Devices").document(deviceIdConcat)
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
