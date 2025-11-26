package com.example.demo.controller;

import com.example.demo.model.Lesson;
import com.example.demo.model.TestCase;
import com.example.demo.model.CodeExecutionResult;
import com.example.demo.service.CodeExecutionService;
import com.example.demo.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;

@Controller
public class MainController {

    @Autowired
    private CodeExecutionService codeExecutionService;

    @Autowired
    private LessonService lessonService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("lessons", lessonService.getAllLessons());
        return "index";
    }

    @GetMapping("/lesson/{id}")
    public String lesson(@PathVariable int id, Model model) {
        Lesson lesson = lessonService.getLessonById(id);
        if (lesson != null) {
            model.addAttribute("theory", lesson.getTheory());
            model.addAttribute("task", lesson.getTask());
            model.addAttribute("lessonId", id);
            return "lesson";
        }
        return "redirect:/";
    }

    @PostMapping("/check-code")
    @ResponseBody
    public CodeExecutionResult checkCode(@RequestParam String code, @RequestParam int lessonId) {
        Lesson lesson = lessonService.getLessonById(lessonId);
        List<TestCase> tests = lesson != null ? lesson.getTests() : Collections.emptyList();
        String fallbackOutput = lesson != null ? lesson.getSolution() : "";
        return codeExecutionService.executeAndCheck(code, tests, fallbackOutput);
    }
}