package com.example.demo.service;

import com.example.demo.model.CodeExecutionResult;
import com.example.demo.model.TestCase;
import com.example.demo.model.TestResult;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class CodeExecutionService {

    private static final int TIMEOUT_SECONDS = 5;

    public CodeExecutionResult executeAndCheck(String userCode, List<TestCase> tests, String fallbackExpected) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("code-run-");
            Path sourceFile = tempDir.resolve("Main.java");
            Files.writeString(sourceFile, userCode, StandardCharsets.UTF_8);

            ExecutionOutcome compileOutcome = runProcess(
                    tempDir,
                    "compile.log",
                    null,
                    "javac",
                    sourceFile.getFileName().toString()
            );
            if (!compileOutcome.success()) {
                return CodeExecutionResult.failure(
                        "Ошибка компиляции",
                        new TestResult("", "", "", false, "Ошибка компиляции:\n" + compileOutcome.output())
                );
            }

            List<TestCase> effectiveTests = selectTests(tests, fallbackExpected);
            List<TestResult> results = new ArrayList<>();

            for (int i = 0; i < effectiveTests.size(); i++) {
                TestCase testCase = effectiveTests.get(i);
                ExecutionOutcome runOutcome = runProcess(
                        tempDir,
                        "run-" + (i + 1) + ".log",
                        testCase.getInput(),
                        "java",
                        "-Xmx64m",
                        "Main"
                );

                if (!runOutcome.success()) {
                    results.add(new TestResult(
                            safeString(testCase.getInput()),
                            safeString(testCase.getExpected()),
                            "",
                            false,
                            runOutcome.output()
                    ));
                    continue;
                }

                String actual = normalize(runOutcome.output());
                String expected = safeString(testCase.getExpected());
                String normalizedExpected = normalize(expected);
                boolean requiresMatch = !expected.isEmpty();
                boolean passed = requiresMatch ? actual.equals(normalizedExpected) : true;

                results.add(new TestResult(
                        safeString(testCase.getInput()),
                        expected,
                        actual,
                        passed,
                        passed ? "" : "Вывод не совпадает"
                ));
            }

            boolean allPassed = results.stream().allMatch(TestResult::isPassed);
            String message = allPassed ? "Все тесты пройдены" : "Есть ошибки в тестах";
            return new CodeExecutionResult(allPassed, results, message);
        } catch (IOException e) {
            return CodeExecutionResult.failure(
                    "Ошибка окружения",
                    new TestResult("", "", "", false, "Ошибка окружения: " + e.getMessage())
            );
        } finally {
            cleanupDirectory(tempDir);
        }
    }

    private List<TestCase> selectTests(List<TestCase> tests, String fallbackExpected) {
        if (tests != null && !tests.isEmpty()) {
            return tests;
        }
        return Collections.singletonList(new TestCase("", fallbackExpected != null ? fallbackExpected : ""));
    }

    private ExecutionOutcome runProcess(Path workingDir, String logFileName, String input, String... command) {
        Path logFile = workingDir.resolve(logFileName);
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                if (input != null && !input.isEmpty()) {
                    writer.write(input);
                }
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return ExecutionOutcome.failure("Превышен лимит времени выполнения (" + TIMEOUT_SECONDS + " с)");
            }

            String output = Files.readString(logFile, StandardCharsets.UTF_8);
            return process.exitValue() == 0
                    ? ExecutionOutcome.success(output)
                    : ExecutionOutcome.failure(output);
        } catch (IOException e) {
            return ExecutionOutcome.failure("Не удалось запустить процесс: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionOutcome.failure("Выполнение прервано");
        }
    }

    private void cleanupDirectory(Path directory) {
        if (directory == null) {
            return;
        }

        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // swallow cleanup errors
                }
            });
        } catch (IOException ignored) {
            // swallow cleanup errors
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").trim();
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private record ExecutionOutcome(boolean success, String output) {
        private static ExecutionOutcome success(String output) {
            return new ExecutionOutcome(true, output);
        }

        private static ExecutionOutcome failure(String output) {
            return new ExecutionOutcome(false, output);
        }
    }
}