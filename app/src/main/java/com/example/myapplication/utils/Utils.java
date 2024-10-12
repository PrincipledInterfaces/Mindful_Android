package com.example.myapplication.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.example.myapplication.Model.Experiment;
import com.example.myapplication.R;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class Utils {

    public static List<Experiment> loadExperimentsData(Context context) {
        List<Experiment> experimentsList = null;

        try {
            // Open the raw resource using the context
            InputStream inputStream = context.getResources().openRawResource(R.raw.predefined_experiments);

            // Use an InputStreamReader to read the file
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);

            // Read the JSON file into a StringBuilder
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            // Log the raw JSON content (optional, for debugging)
            Log.d("JSON Data", jsonStringBuilder.toString());

            // Now parse the JSON using Gson
            Type experimentListType = new TypeToken<List<Experiment>>() {}.getType();
            experimentsList = new Gson().fromJson(jsonStringBuilder.toString(), experimentListType);

            if (experimentsList == null) {
                Log.e("ErrorJson", "Parsed list is null.");
            } else {
                Log.d("Success", "Experiments loaded successfully.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("ErrorJson", "Resource not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("ErrorJson", "Error reading the file: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            Log.e("ErrorJson", "Error parsing JSON: " + e.getMessage());
        }

        return experimentsList;
    }
}
