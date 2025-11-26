package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class Lesson {
    private int id;
    private String title;
    private String description;
    private String theory;
    private String task;
    private String solution;
    private List<TestCase> tests = new ArrayList<>();

    // Конструктор по умолчанию (важен для Jackson)
    public Lesson() {}

    // Конструктор с параметрами (ДОБАВЬ ЭТОТ!)
    public Lesson(int id, String title, String description, String theory, String task, String solution) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.theory = theory;
        this.task = task;
        this.solution = solution;
    }

    // Геттеры
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTheory() { return theory; }
    public String getTask() { return task; }
    public String getSolution() { return solution; }
    public List<TestCase> getTests() { return tests; }

    public void setTests(List<TestCase> tests) {
        this.tests = tests != null ? tests : new ArrayList<>();
    }
}
