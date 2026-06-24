package com.demo.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AppController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Automated Deployment Pipeline - Backend Running");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        return response;
    }

    @GetMapping("/api/version")
    public Map<String, String> version() {
        Map<String, String> response = new HashMap<>();
        response.put("version", "1.0.0");
        response.put("environment", System.getenv().getOrDefault("APP_ENV", "production"));
        return response;
    }
}
