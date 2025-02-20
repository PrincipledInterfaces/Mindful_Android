package com.example.myapplication.Model;

import com.example.myapplication.Model.Survey.QuestionAnswer;

import java.util.List;

public class SurveyDetails {
    private List<QuestionAnswer> questionsAndAnswers;
    private List<String> selectedApps;
    private String timestamp;
    public SurveyDetails(List<QuestionAnswer> questionsAndAnswers, List<String> selectedApps, String timestamp) {
        this.questionsAndAnswers = questionsAndAnswers;
        this.selectedApps = selectedApps;
        this.timestamp = timestamp;
    }

    // Add getters
    public List<QuestionAnswer> getQuestionsAndAnswers() {
        return questionsAndAnswers;
    }

    public List<String> getSelectedApps() {
        return selectedApps;
    }

    public String getTimestamp() {
        return timestamp;
    }
}



