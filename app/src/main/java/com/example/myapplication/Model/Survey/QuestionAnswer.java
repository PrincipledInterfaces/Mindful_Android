package com.example.myapplication.Model.Survey;

public class QuestionAnswer {
    private String question;
    private String answer;

    public QuestionAnswer(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    // Add getters
    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }
}
