package com.example.myapplication.Model.Survey;

import java.util.List;

public class MonthlySurvey {
    private List<QuestionAnswer> questionsAndAnswers;

    private long timestamp;
    public MonthlySurvey(List<QuestionAnswer> questionsAndAnswers, long timestamp) {
        this.questionsAndAnswers = questionsAndAnswers;
        this.timestamp = timestamp;
    }

    // Add getters
    public List<QuestionAnswer> getQuestionsAndAnswers() {
        return questionsAndAnswers;
    }


    public long getTimestamp() {
        return timestamp;
    }
}

