package com.example.myapplication.Model;

import java.util.List;

public class Experiment {
    public int id;        // Unique ID for each experiment
    public String title;  // Title of the experiment
    public String goal;   // Goal of the experiment
    public List<String> steps;  // List of steps for the experiment

    // Optionally, add a constructor for easier object creation
    public Experiment(int id, String title, String goal, List<String> steps) {
        this.id = id;
        this.title = title;
        this.goal = goal;
        this.steps = steps;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }
}
