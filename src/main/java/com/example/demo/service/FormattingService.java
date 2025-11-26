package com.example.demo.service;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.springframework.stereotype.Service;

@Service
public class FormattingService {

    private final Formatter formatter = new Formatter();

    public String formatJava(String code) throws FormatterException {
        return formatter.formatSource(code);
    }
}

