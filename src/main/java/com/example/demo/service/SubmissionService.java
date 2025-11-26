package com.example.demo.service;

import com.example.demo.model.CodeExecutionResult;
import com.example.demo.model.Lesson;
import com.example.demo.model.Submission;
import com.example.demo.model.SubmissionRequest;
import com.example.demo.model.SubmissionStatus;
import com.example.demo.model.TestCase;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SubmissionService {

    private final LessonService lessonService;
    private final CodeExecutionService codeExecutionService;
    private final ExecutorService executorService;
    private final Map<String, Submission> submissions = new ConcurrentHashMap<>();

    public SubmissionService(LessonService lessonService, CodeExecutionService codeExecutionService) {
        this.lessonService = lessonService;
        this.codeExecutionService = codeExecutionService;
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(threads);
    }

    public Submission submit(SubmissionRequest request) {
        String code = request.getCode() == null ? "" : request.getCode();
        String language = request.getLanguage() == null ? "java" : request.getLanguage();
        Lesson lesson = lessonService.getLessonById(request.getLessonId());
        String submissionId = UUID.randomUUID().toString();
        Submission submission = new Submission(submissionId, request.getLessonId(), language, SubmissionStatus.QUEUED);
        submissions.put(submissionId, submission);

        if (lesson == null) {
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setError("Урок с id=" + request.getLessonId() + " не найден");
            return submission;
        }

        executorService.submit(() -> processSubmission(submission, lesson, code));
        return submission;
    }

    public Submission getSubmission(String id) {
        return submissions.get(id);
    }

    private void processSubmission(Submission submission, Lesson lesson, String code) {
        submission.setStatus(SubmissionStatus.RUNNING);
        try {
            List<TestCase> tests = lesson.getTests() == null ? Collections.emptyList() : lesson.getTests();
            CodeExecutionResult result = codeExecutionService.executeAndCheck(code, tests, lesson.getSolution());
            submission.setResult(result);
            submission.setStatus(SubmissionStatus.COMPLETED);
        } catch (Exception e) {
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setError("Ошибка выполнения: " + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }
}

