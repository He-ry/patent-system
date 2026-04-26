package com.example.patent.controller;

import com.example.patent.dto.ChatRequest;
import com.example.patent.dto.SuggestionRequest;
import com.example.patent.service.ChatService;
import com.example.patent.service.PatentIndexService;
import com.example.patent.vo.ChatEventVO;
import com.example.patent.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final PatentIndexService patentIndexService;

    @GetMapping("/conversations")
    public List<ConversationVO> getConversations() {
        return chatService.getConversations();
    }

    @GetMapping("/conversation/{id}")
    public ConversationVO getConversation(@PathVariable String id) {
        return chatService.getConversation(id);
    }

    @DeleteMapping("/conversation/{id}")
    public void deleteConversation(@PathVariable String id) {
        chatService.deleteConversation(id);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        return chatService.chatSse(request.getConversationId(), request.getContent());
    }

    @PostMapping("/index/patents")
    public Map<String, Object> indexPatents() {
        log.info("手动触发专利索引");
        long count = patentIndexService.indexAllPatents();
        return Map.of("indexed", count, "message", "专利索引完成");
    }

    @PostMapping("/suggestions")
    public Map<String, Object> getSuggestions(@RequestBody SuggestionRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return Map.of("suggestions", List.of(
                    "查看专利总量和技术领域分布",
                    "分析近五年申请趋势",
                    "查询高价值专利排名",
                    "生成专利分析报告"
            ));
        }
        List<String> suggestions = chatService.generateSuggestions(conversationId);
        return Map.of("suggestions", suggestions);
    }
}
