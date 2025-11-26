package com.example.demo.model;

public enum TestMode {
    EXACT,
    NUMBER_SYSTEMS;

    public static TestMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return EXACT;
        }
        try {
            return TestMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EXACT;
        }
    }
}

