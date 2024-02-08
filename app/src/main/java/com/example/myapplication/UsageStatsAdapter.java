package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UsageStatsAdapter extends RecyclerView.Adapter<UsageStatsAdapter.ViewHolder> {
    private List<UsageStatsModel> appUsageInfoList;

    public UsageStatsAdapter(List<UsageStatsModel> appUsageInfoList) {
        this.appUsageInfoList = appUsageInfoList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView packageNameTextView;
        TextView usageDurationTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            packageNameTextView = itemView.findViewById(R.id.usageTimeTextView);
            usageDurationTextView = itemView.findViewById(R.id.appNameTextView);
        }
    }

    @NonNull
    @Override
    public UsageStatsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsageStatsAdapter.ViewHolder holder, int position) {
        UsageStatsModel appUsageInfo = appUsageInfoList.get(position);
        holder.packageNameTextView.setText(appUsageInfo.getPackageName());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(appUsageInfo.getUsageDuration());
        holder.usageDurationTextView.setText(minutes + " minutes");
    }

    @Override
    public int getItemCount() {
        return appUsageInfoList.size();
    }
}
