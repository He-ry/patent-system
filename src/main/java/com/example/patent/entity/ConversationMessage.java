package com.example.patent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversation_message")
public class ConversationMessage {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String conversationId;

    private String role;

    private String content;

    private Integer messageOrder;

    private Integer likes;

    private Integer dislikes;

    private String briefSummary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}