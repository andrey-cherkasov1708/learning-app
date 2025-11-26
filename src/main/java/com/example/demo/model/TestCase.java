package com.example.demo.model;

public class TestCase {
    private String input;
    private String expected;

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

    public void setInput(String input) {
        this.input = input;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }
}

