package com.example.demo.model;

import java.time.Instant;

public class Submission {
    private final String id;
    private final int lessonId;
    private final String language;
    private volatile SubmissionStatus status;
    private volatile CodeExecutionResult result;
    private volatile String error;
    private final Instant createdAt;

    public Submission(String id, int lessonId, String language, SubmissionStatus status) {
        this.id = id;
        this.lessonId = lessonId;
        this.language = language;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public int getLessonId() {
        return lessonId;
    }

    public String getLanguage() {
        return language;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public CodeExecutionResult getResult() {
        return result;
    }

    public void setResult(CodeExecutionResult result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

