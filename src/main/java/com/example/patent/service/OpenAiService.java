package com.example.patent.service;

import com.example.patent.config.OpenAiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {
    private final OpenAiConfig openAiConfig;
    private final ObjectMapper objectMapper;

    @Value("${report.output.path:./reports}/../config/ai-model.json")
    private String configFile;

    private volatile Map<String, Object> fileConfigCache;
    private volatile long lastModified = 0;
    private volatile long cacheTime = 0;

    @SuppressWarnings("unchecked")
    private synchronized Map<String, Object> getFileConfig() {
        File file = new File(configFile);
        if (!file.exists()) return Collections.emptyMap();
        if (file.lastModified() <= lastModified && System.currentTimeMillis() - cacheTime < 30000) {
            return fileConfigCache != null ? fileConfigCache : Collections.emptyMap();
        }
        try {
            String content = Files.readString(file.toPath());
            fileConfigCache = objectMapper.readValue(content, LinkedHashMap.class);
            lastModified = file.lastModified();
            cacheTime = System.currentTimeMillis();
            log.debug("AI model config reloaded from: {}", configFile);
            return fileConfigCache;
        } catch (Exception e) {
            log.warn("Failed to load AI model config: {}", e.getMessage());
            return fileConfigCache != null ? fileConfigCache : Collections.emptyMap();
        }
    }

    private String resolve(String key, String defaultValue) {
        Map<String, Object> fc = getFileConfig();
        Object val = fc.get(key);
        if (val instanceof String && !((String) val).isBlank()) return (String) val;
        return defaultValue;
    }

    public List<Float> getEmbedding(String text) {
        List<List<Float>> results = getEmbeddings(List.of(text));
        return results.isEmpty() ? new ArrayList<>() : results.get(0);
    }

    public List<List<Float>> getEmbeddings(List<String> texts) {
        String ollamaUrl = resolve("ollamaBaseUrl", openAiConfig.getOllamaBaseUrl());
        String embeddingModel = resolve("embeddingModel", openAiConfig.getEmbeddingModel());

        WebClient client = WebClient.builder()
                .baseUrl(ollamaUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();

        int poolSize = Math.min(10, texts.size());
        var executor = Executors.newFixedThreadPool(poolSize);

        List<CompletableFuture<List<Float>>> futures = new ArrayList<>();
        for (String text : texts) {
            CompletableFuture<List<Float>> future = CompletableFuture.supplyAsync(() -> {
                Map<String, Object> body = new HashMap<>();
                body.put("model", embeddingModel);
                body.put("prompt", text);

                try {
                    String response = client.method(org.springframework.http.HttpMethod.POST)
                            .uri("/api/embeddings")
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    return parseEmbeddingResponse(response);
                } catch (Exception e) {
                    log.error("Embedding failed for text: {}, error: {}", text, e.getMessage());
                    return new ArrayList<Float>();
                }
            }, executor);
            futures.add(future);
        }

        executor.shutdown();
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    private List<Float> parseEmbeddingResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode embedding = root.path("embedding");
            List<Float> result = new ArrayList<>();
            if (embedding.isArray()) {
                for (JsonNode node : embedding) {
                    result.add((float) node.asDouble());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("解析embedding失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String getModel() {
        return resolve("model", openAiConfig.getModel());
    }

    public String chat(String systemPrompt, String userContent) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));
        body.put("temperature", 0.1);

        WebClient client = getWebClient();
        String response = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractContent(response);
    }

    public String chatWithJson(String systemPrompt, String userContent) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));
        body.put("temperature", 0.1);
        body.put("response_format", Map.of("type", "json_object"));

        WebClient client = getWebClient();
        String response = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractContent(response);
    }

    public Flux<String> chatStream(List<Map<String, String>> messages) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", getModel());
        body.put("messages", messages);
        body.put("temperature", 0.1);
        body.put("stream", true);

        WebClient client = getWebClient();
        AtomicReference<String> buffer = new AtomicReference<>("");

        return client.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .concatMap(text -> parseSSE(text, buffer))
                .doOnComplete(() -> log.info("[AI] 流式响应完成"))
                .doOnError(e -> log.error("[AI] 流式响应失败: {}", e.getMessage()));
    }

    private Flux<String> parseSSE(String text, AtomicReference<String> buffer) {
        String combined = buffer.get() + text;
        StringBuilder result = new StringBuilder();
        int newlineIdx;

        while ((newlineIdx = combined.indexOf('\n')) >= 0) {
            String line = combined.substring(0, newlineIdx).trim();
            combined = combined.substring(newlineIdx + 1);

            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) continue;
                String content = extractDeltaContent(data);
                if (content != null) result.append(content);
            }
        }
        buffer.set(combined);
        return result.length() > 0 ? Flux.just(result.toString()) : Flux.empty();
    }

    private String extractDeltaContent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("choices").get(0).path("delta").path("content").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractContent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.path("choices").get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage());
            return null;
        }
    }

    private WebClient getWebClient() {
        String apiKey = resolve("apiKey", openAiConfig.getApiKey());
        String baseUrl = resolve("baseUrl", openAiConfig.getBaseUrl());

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}
