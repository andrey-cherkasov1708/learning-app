package com.example.demo.model;

public class TestResult {
    private final String input;
    private final String expected;
    private final String output;
    private final boolean passed;
    private final String error;
    private final String details;

    public TestResult(String input, String expected, String output, boolean passed, String error) {
        this(input, expected, output, passed, error, "");
    }

    public TestResult(String input, String expected, String output, boolean passed, String error, String details) {
        this.input = input;
        this.expected = expected;
        this.output = output;
        this.passed = passed;
        this.error = error;
        this.details = details;
    }

    public String getInput() {
        return input;
    }

    public String getExpected() {
        return expected;
    }

    public String getOutput() {
        return output;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getError() {
        return error;
    }

    public String getDetails() {
        return details;
    }
}

