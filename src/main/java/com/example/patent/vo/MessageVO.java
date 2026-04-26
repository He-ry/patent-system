package com.example.patent.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageVO {
    private String id;
    private String conversationId;
    private String role;
    private String content;
    private Integer messageOrder;
    private Integer likes;
    private Integer dislikes;
    private String briefSummary;
    private LocalDateTime createdAt;
    private List<ReferenceVO> references;

    @Data
    public static class ReferenceVO {
        private String id;
        private String messageId;
        private String docId;
        private String docTitle;
        private String content;
        private BigDecimal relevanceScore;
    }
}
