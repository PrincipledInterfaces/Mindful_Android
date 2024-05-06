package com.example.myapplication.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Model.DeviceEvent;
import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeviceEventAdapter extends RecyclerView.Adapter<DeviceEventAdapter.ViewHolder> {

    private List<DeviceEvent> eventList;
    private Context context;
    private LayoutInflater inflater; // Add the inflater here

    // Constructor
    public DeviceEventAdapter(Context context, List<DeviceEvent> eventList) {
        this.context = context;
        this.eventList = (eventList != null) ? eventList : new ArrayList<>();
        this.inflater = LayoutInflater.from(context); // Initialize the inflater here
    }


    // ViewHolder inner class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventTypeTextView;
        TextView timeTextView, durationTextView;

        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTypeTextView = itemView.findViewById(R.id.eventType);
            timeTextView = itemView.findViewById(R.id.time);
            durationTextView = itemView.findViewById(R.id.timeDuration);
            progressBar = itemView.findViewById(R.id.progressBar2);
            progressBar.setMax(100);

        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.event_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceEvent event = eventList.get(position);
        holder.eventTypeTextView.setText(event.getEventType());
//        holder.timeTextView.setText(String.valueOf(event.getTime())); // Format time as needed
        holder.timeTextView.setText(formatTime(event.getTime()));
        holder.durationTextView.setText("Duration: " + event.getDuration() + "s");
        holder.progressBar.setProgress((int) event.getDuration());

    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }
}
