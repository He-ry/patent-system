package com.example.patent.controller;

import com.example.patent.service.OpenAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ModelConfigController {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
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
            openAiService.refreshConfig();
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

    @PostMapping("/test")
    public Map<String, Object> testConnection(@RequestBody Map<String, Object> config) {
        String apiKey = (String) config.get("apiKey");
        String baseUrl = (String) config.get("baseUrl");
        String model = (String) config.get("model");

        log.info("[AI连接测试] 开始测试连接 - baseUrl: {}, model: {}", baseUrl, model);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[AI连接测试] 失败: API Key 为空");
            return Map.of("success", false, "message", "API Key 不能为空");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("[AI连接测试] 失败: Base URL 为空");
            return Map.of("success", false, "message", "Base URL 不能为空");
        }
        if (model == null || model.isBlank()) {
            log.warn("[AI连接测试] 失败: 模型名称为空");
            return Map.of("success", false, "message", "模型名称不能为空");
        }

        if (apiKey.contains("****")) {
            Map<String, Object> existing = loadConfig();
            apiKey = (String) existing.getOrDefault("apiKey", apiKey);
            log.info("[AI连接测试] 使用已保存的 API Key 进行测试");
        }

        try {
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(Map.of("role", "user", "content", "Hi")));
            body.put("max_tokens", 5);

            log.info("[AI连接测试] 发送请求到: {}/chat/completions", baseUrl);

            String response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String responseModel = root.path("model").asText("");
            String content = root.path("choices").get(0).path("message").path("content").asText("");

            log.info("[AI连接测试] 连接成功 - 响应模型: {}, 内容: {}", responseModel, content);
            return Map.of("success", true, "message", "连接成功！模型: " + responseModel);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("[AI连接测试] 连接失败: {}", errorMsg, e);

            if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
                return Map.of("success", false, "message", "认证失败：API Key 无效");
            } else if (errorMsg.contains("404")) {
                return Map.of("success", false, "message", "接口不存在：请检查 Base URL");
            } else if (errorMsg.contains("Connection refused") || errorMsg.contains("connect")) {
                return Map.of("success", false, "message", "网络连接失败：请检查 Base URL 是否正确");
            } else if (errorMsg.contains("timeout")) {
                return Map.of("success", false, "message", "连接超时：请检查网络或服务状态");
            }

            return Map.of("success", false, "message", "连接失败: " + errorMsg);
        }
    }
}
