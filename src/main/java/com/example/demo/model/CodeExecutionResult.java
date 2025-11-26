package com.example.demo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeExecutionResult {
    private final boolean correct;
    private final List<TestResult> tests;
    private final String message;

    public CodeExecutionResult(boolean correct, List<TestResult> tests, String message) {
        this.correct = correct;
        this.tests = tests != null ? tests : new ArrayList<>();
        this.message = message;
    }

    public static CodeExecutionResult failure(String message, TestResult result) {
        return new CodeExecutionResult(false, Collections.singletonList(result), message);
    }

    public boolean isCorrect() {
        return correct;
    }

    public List<TestResult> getTests() {
        return tests;
    }

    public String getMessage() {
        return message;
    }
}