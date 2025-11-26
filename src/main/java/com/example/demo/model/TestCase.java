package com.example.demo.model;

public class TestCase {
    private String input;
    private String expected;
    private String mode;

    public TestCase() {
    }

    public TestCase(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    public String getInput() {
        return input;
    }

    public String getExpected() {
        return expected;
    }

    public String getMode() {
        return mode;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}

