package com.example.myapplication.Model.Survey;

import java.util.List;

public class MonthlySurvey {
    private List<QuestionAnswer> questionsAndAnswers;

    private String timestamp;
    public MonthlySurvey(List<QuestionAnswer> questionsAndAnswers, String timestamp) {
        this.questionsAndAnswers = questionsAndAnswers;
        this.timestamp = timestamp;
    }

    // Add getters
    public List<QuestionAnswer> getQuestionsAndAnswers() {
        return questionsAndAnswers;
    }


    public String getTimestamp() {
        return timestamp;
    }
}

