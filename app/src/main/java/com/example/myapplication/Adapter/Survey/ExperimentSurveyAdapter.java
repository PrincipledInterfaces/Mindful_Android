package com.example.myapplication.Adapter.Survey;

// ExperimentAdapter.java
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Model.Experiment;
import com.example.myapplication.Model.Survey.LaunchSurveyResponse;
import com.example.myapplication.R;

import java.util.List;

public class ExperimentSurveyAdapter extends RecyclerView.Adapter<ExperimentSurveyAdapter.ExperimentViewHolder> {

    private List<Experiment> experimentList;
    private List<LaunchSurveyResponse> responseList;

    public ExperimentSurveyAdapter(List<Experiment> experimentList, List<LaunchSurveyResponse> responseList) {
        this.experimentList = experimentList;
        this.responseList = responseList;
    }

    @NonNull
    @Override
    public ExperimentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_experiment_survey, parent, false);
        return new ExperimentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ExperimentViewHolder holder, int position) {
        Experiment experiment = experimentList.get(position);
        holder.bind(experiment, responseList.get(position));
    }

    @Override
    public int getItemCount() {
        return experimentList.size();
    }

    public List<LaunchSurveyResponse> getResponses() {
        return responseList;
    }

    public class ExperimentViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private SeekBar seekBarWillingness, seekBarImpact, seekBarConfidence;
        private int willingness = 3, impact = 3, confidence = 3; // Default values

        public ExperimentViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.experiment_title);
            seekBarWillingness = itemView.findViewById(R.id.seekbar_willingness);
            seekBarImpact = itemView.findViewById(R.id.seekbar_impact);
            seekBarConfidence = itemView.findViewById(R.id.seekbar_confidence);
        }

        public void bind(final Experiment experiment, final LaunchSurveyResponse response) {
            title.setText(experiment.getTitle());

            // Initialize SeekBars with existing responses
            seekBarWillingness.setProgress(response.getWillingness() - 1);
            seekBarImpact.setProgress(response.getImpact() - 1);
            seekBarConfidence.setProgress(response.getConfidence() - 1);

            // Set listeners to update responses
            seekBarWillingness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    willingness = progress + 1;
                    responseList.set(getAdapterPosition(), new LaunchSurveyResponse(
                            experiment.getTitle(),
                            willingness,
                            response.getImpact(),
                            response.getConfidence()
                    ));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });

            seekBarImpact.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    impact = progress + 1;
                    responseList.set(getAdapterPosition(), new LaunchSurveyResponse(
                            experiment.getTitle(),
                            response.getWillingness(),
                            impact,
                            response.getConfidence()
                    ));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });

            seekBarConfidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    confidence = progress + 1;
                    responseList.set(getAdapterPosition(), new LaunchSurveyResponse(
                            experiment.getTitle(),
                            response.getWillingness(),
                            response.getImpact(),
                            confidence
                    ));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }
    }
}
