package com.example.patent.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String conversationId;
    private String content;
}