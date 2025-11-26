package com.example.demo.service;

import com.example.demo.model.CodeExecutionResult;
import com.example.demo.model.TestCase;
import com.example.demo.model.TestResult;
import com.example.demo.model.TestMode;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
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

                results.add(evaluateTestCase(testCase, runOutcome.output()));
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

    private TestResult evaluateTestCase(TestCase testCase, String rawOutput) {
        String actual = normalize(rawOutput);
        String expected = safeString(testCase.getExpected());
        TestMode mode = TestMode.from(testCase.getMode());

        return switch (mode) {
            case NUMBER_SYSTEMS -> evaluateNumberSystems(testCase, actual);
            case EXACT -> {
                String normalizedExpected = normalize(expected);
                boolean requiresMatch = !expected.isEmpty();
                boolean passed = requiresMatch ? actual.equals(normalizedExpected) : true;
                yield new TestResult(
                        safeString(testCase.getInput()),
                        expected.isEmpty() ? "<любой вывод>" : expected,
                        actual,
                        passed,
                        passed ? "" : "Вывод не совпадает"
                );
            }
        };
    }

    private TestResult evaluateNumberSystems(TestCase testCase, String actualOutput) {
        String input = safeString(testCase.getInput()).trim();
        int value;
        try {
            value = Integer.parseInt(input.split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return new TestResult(input, "", actualOutput, false, "Не удалось разобрать входное значение", "");
        }

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("binary", Integer.toBinaryString(value));
        expectedMap.put("octal", Integer.toOctalString(value));
        expectedMap.put("hex", Integer.toHexString(value).toUpperCase(Locale.ROOT));
        expectedMap.put("reciprocal", value == 0 ? "Infinity" : Double.toHexString(1.0 / value));

        Map<String, String> actualMap = parseNumberSystemOutput(actualOutput);

        StringBuilder details = new StringBuilder();
        boolean success = true;
        for (Map.Entry<String, String> entry : expectedMap.entrySet()) {
            String key = entry.getKey();
            String actual = actualMap.get(key);
            if (actual == null) {
                success = false;
                details.append(MessageFormat.format("Не найдена строка для {0}. ", key));
                continue;
            }
            String expectedValue = entry.getValue();
            if (key.equals("hex")) {
                actual = actual.toUpperCase(Locale.ROOT);
            }
            if (!expectedValue.equals(actual)) {
                success = false;
                details.append(MessageFormat.format("Ожидалось {0}={1}, получено {2}. ", key, expectedValue, actual));
            }
        }

        String expectedDisplay = String.format(
                Locale.ROOT,
                "Binary: %s%nOctal: %s%nHex: %s%nHex float reciprocal: %s",
                expectedMap.get("binary"),
                expectedMap.get("octal"),
                expectedMap.get("hex"),
                expectedMap.get("reciprocal")
        );

        if (success) {
            return new TestResult(input, expectedDisplay, actualOutput, true, "", details.toString());
        }
        return new TestResult(input, expectedDisplay, actualOutput, false, "Значения не совпадают", details.toString());
    }

    private Map<String, String> parseNumberSystemOutput(String output) {
        Map<String, String> map = new HashMap<>();
        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            String normalized = line.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (lower.contains("binary") || lower.contains("двоич")) {
                map.put("binary", extractValue(normalized));
            } else if (lower.contains("octal") || lower.contains("восьмер")) {
                map.put("octal", extractValue(normalized));
            } else if ((lower.contains("hex float reciprocal")) || lower.contains("обратн")) {
                map.put("reciprocal", extractValue(normalized));
            } else if (lower.contains("hex") || lower.contains("шестнадц")) {
                map.put("hex", extractValue(normalized));
            }
        }
        return map;
    }

    private String extractValue(String line) {
        int idx = line.indexOf(':');
        if (idx >= 0 && idx + 1 < line.length()) {
            return line.substring(idx + 1).trim();
        }
        String[] tokens = line.split("\\s+");
        return tokens.length == 0 ? "" : tokens[tokens.length - 1];
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