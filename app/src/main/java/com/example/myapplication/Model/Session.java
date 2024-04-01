package com.example.myapplication.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Session {
    public String startTime;
     public String endTime = "Ongoing";
    public List<Map<String, Object>> events = new ArrayList<>();

    public Session(String startTime) {
        this.startTime = startTime;
    }
}
