package com.example.myapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.Model.UsageStatsModel;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class UsageStatsAdapter extends RecyclerView.Adapter<UsageStatsAdapter.ViewHolder> {
    private List<UsageStatsModel> appUsageInfoList;

    public UsageStatsAdapter(List<UsageStatsModel> appUsageInfoList) {
        this.appUsageInfoList = appUsageInfoList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appNameTextView; // Renamed for clarity
        TextView usageDurationTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            appNameTextView = itemView.findViewById(R.id.appNameTextView); // Make sure this ID matches in your layout
            usageDurationTextView = itemView.findViewById(R.id.usageTimeTextView); // Ensure correct ID is used
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsageStatsModel appUsageInfo = appUsageInfoList.get(position);
        holder.appNameTextView.setText(appUsageInfo.getAppName()); // Display the app name
        long minutes = TimeUnit.MILLISECONDS.toMinutes(appUsageInfo.getUsageDuration());
        holder.usageDurationTextView.setText(minutes + " minutes");
    }

    @Override
    public int getItemCount() {
        return appUsageInfoList.size();
    }
}
