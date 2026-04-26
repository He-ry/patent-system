package com.example.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaFallbackController {

    @GetMapping(value = "/{path:[^\\.]+}")
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/") || uri.startsWith("/upload")) {
            return null;
        }
        return "forward:/index.html";
    }
}
