package com.example.demo.controller;

import com.example.demo.model.FormatRequest;
import com.example.demo.model.FormatResponse;
import com.example.demo.model.Submission;
import com.example.demo.model.SubmissionRequest;
import com.example.demo.model.SubmissionResponse;
import com.example.demo.service.FormattingService;
import com.example.demo.service.SubmissionService;
import com.google.googlejavaformat.java.FormatterException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final FormattingService formattingService;

    public SubmissionController(SubmissionService submissionService, FormattingService formattingService) {
        this.submissionService = submissionService;
        this.formattingService = formattingService;
    }

    @PostMapping("/submissions")
    public SubmissionResponse createSubmission(@RequestBody SubmissionRequest request) {
        Submission submission = submissionService.submit(request);
        return new SubmissionResponse(submission.getId(), submission.getStatus());
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<Submission> getSubmission(@PathVariable String id) {
        Submission submission = submissionService.getSubmission(id);
        if (submission == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/format")
    public ResponseEntity<?> formatCode(@RequestBody FormatRequest request) {
        try {
            String formatted = formattingService.formatJava(request.getCode());
            return ResponseEntity.ok(new FormatResponse(formatted));
        } catch (FormatterException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new FormatResponse("/* Ошибка форматирования: " + e.getMessage() + " */"));
        }
    }
}

