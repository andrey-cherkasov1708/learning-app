package com.example.demo.service;

import com.example.demo.model.CodeExecutionResult;
import com.example.demo.model.TestCase;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeExecutionServiceTest {

    private final CodeExecutionService service = new CodeExecutionService();

    @Test
    void executesAndValidatesCorrectSolution() {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.print("Hello Java!");
                    }
                }
                """;

        List<TestCase> tests = Collections.singletonList(new TestCase("", "Hello Java!"));
        CodeExecutionResult result = service.executeAndCheck(code, tests, "");

        assertTrue(result.isCorrect());
        assertEquals(1, result.getTests().size());
        assertTrue(result.getTests().get(0).isPassed());
        assertEquals("Hello Java!", result.getTests().get(0).getOutput());
    }

    @Test
    void returnsCompilerErrorsAsOutput() {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.print("Missing brace");
                """;

        CodeExecutionResult result = service.executeAndCheck(code, Collections.emptyList(), "");

        assertFalse(result.isCorrect());
        assertTrue(result.getMessage().contains("Ошибка компиляции"));
        assertFalse(result.getTests().isEmpty());
        assertTrue(result.getTests().get(0).getError().contains("Ошибка компиляции"));
    }
}

