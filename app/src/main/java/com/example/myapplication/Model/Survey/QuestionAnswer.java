package com.example.myapplication.Model.Survey;

import java.util.List;

public class QuestionAnswer {
    private String question;
    private String answer;
    private List<QuestionAnswer> qa;

    public QuestionAnswer(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.qa = null; // No nested QuestionAnswer by default
    }

    public QuestionAnswer(String question, List<QuestionAnswer> qa) {
        this.question = question;
        this.qa = qa;
        this.answer = null;
    }

    // Add getters
    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer != null ? answer : "";
    }

    public List<QuestionAnswer> getQA() {
        return qa;
    }
}
