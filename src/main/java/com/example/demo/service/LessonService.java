package com.example.demo.service;


import com.example.demo.model.Lesson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LessonService {
    private List<Lesson> lessons = new ArrayList<>();

    @PostConstruct
    public void loadLessons() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("lessons.json");
            try (InputStream inputStream = resource.getInputStream()) {
                lessons = Arrays.asList(mapper.readValue(inputStream, Lesson[].class));
            }
        } catch (IOException e) {
            // Заглушка если файл не найден
            lessons = Arrays.asList(
                    new Lesson(1, "Введение", "Первый урок", "Теория...", "Задание...", "Решение")
            );
        }
    }

    public List<Lesson> getAllLessons() {
        return lessons;
    }

    public Lesson getLessonById(int id) {
        return lessons.stream()
                .filter(lesson -> lesson.getId() == id)
                .findFirst()
                .orElse(null);
    }
}
