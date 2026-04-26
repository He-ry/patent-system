package com.example.patent.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationVO {
    private String id;
    private String userId;
    private String title;
    private String status;
    private String patentIds;
    private String summary;
    private String uploadedFiles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageVO> messages;
    private Long messageCount;
}