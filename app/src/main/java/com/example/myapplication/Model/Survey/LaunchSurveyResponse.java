package com.example.myapplication.Model.Survey;

public class LaunchSurveyResponse {
//    private int experimentId;
    private String experimentTitle;
    private int willingness; // 1-5
    private int impact;      // 1-5
    private int confidence;  // 1-5

    public LaunchSurveyResponse(String experimentTitle, int willingness, int impact, int confidence) {
        this.experimentTitle = experimentTitle;
        this.willingness = willingness;
        this.impact = impact;
        this.confidence = confidence;
    }

    public String getExperimentTitle(){
        return experimentTitle;
    }

//    public int getExperimentId() {
//        return experimentId;
//    }

    public int getWillingness() {
        return willingness;
    }

    public int getImpact() {
        return impact;
    }

    public int getConfidence() {
        return confidence;
    }
}

