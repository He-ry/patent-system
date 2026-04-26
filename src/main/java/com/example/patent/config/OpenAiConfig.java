package com.example.patent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {
    private String apiKey;
    private String baseUrl;
    private String model;
    private String embeddingModel;
    private String ollamaBaseUrl;
}