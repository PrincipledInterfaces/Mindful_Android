package com.example.myapplication.Model;

import java.util.List;

public class Experiment {
    public int id;        // Unique ID for each experiment
    public String title;  // Title of the experiment
    public String goal;   // Goal of the experiment
    public List<String> steps;  // List of steps for the experiment

    public int reduceOverallTime;
    public int reduceAppTime;
    public int reduceUnlockTime;
    public int reduceCheckFrequency;

    // Optionally, add a constructor for easier object creation
    public Experiment(int id, String title, String goal, List<String> steps, int reduceOverallTime, int reduceAppTime, int reduceUnlockTime, int reduceCheckFrequency) {
        this.id = id;
        this.title = title;
        this.goal = goal;
        this.steps = steps;

        this.reduceOverallTime = reduceOverallTime;
        this.reduceAppTime = reduceAppTime;
        this.reduceUnlockTime = reduceUnlockTime;
        this.reduceCheckFrequency = reduceCheckFrequency;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }
}
