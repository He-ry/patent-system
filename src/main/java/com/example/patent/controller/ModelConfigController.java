package com.example.patent.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ModelConfigController {

    private static final String CONFIG_FILE = "config/ai-model.json";

    @GetMapping("/model")
    public Map<String, Object> getModelConfig() {
        Map<String, Object> config = loadConfig();
        // Mask API key for security
        if (config.containsKey("apiKey")) {
            String key = (String) config.get("apiKey");
            if (key != null && key.length() > 8) {
                config.put("apiKey", key.substring(0, 4) + "****" + key.substring(key.length() - 4));
            }
        }
        return config;
    }

    @PostMapping("/model")
    public Map<String, Object> saveModelConfig(@RequestBody Map<String, Object> config) {
        try {
            Path configDir = Paths.get(CONFIG_FILE).getParent();
            if (configDir != null) Files.createDirectories(configDir);

            // Mask handling: if the key contains ****, keep the old key
            Map<String, Object> existing = loadConfig();
            String newKey = (String) config.get("apiKey");
            if (newKey != null && newKey.contains("****")) {
                config.put("apiKey", existing.getOrDefault("apiKey", newKey));
            }

            Files.writeString(Paths.get(CONFIG_FILE), new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(config));
            log.info("Model config saved");
            return Map.of("success", true, "message", "配置已保存");
        } catch (Exception e) {
            log.error("Failed to save model config", e);
            return Map.of("success", false, "message", "保存失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadConfig() {
        try {
            File file = new File(CONFIG_FILE);
            if (file.exists()) {
                String content = Files.readString(file.toPath());
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(content, LinkedHashMap.class);
            }
        } catch (Exception e) {
            log.warn("Failed to load model config: {}", e.getMessage());
        }
        return new LinkedHashMap<>();
    }
}
