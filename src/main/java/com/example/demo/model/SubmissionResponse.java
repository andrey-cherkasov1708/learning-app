package com.example.demo.model;

public class SubmissionResponse {
    private final String submissionId;
    private final SubmissionStatus status;

    public SubmissionResponse(String submissionId, SubmissionStatus status) {
        this.submissionId = submissionId;
        this.status = status;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }
}

