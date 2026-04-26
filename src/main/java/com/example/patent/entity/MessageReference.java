package com.example.patent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("message_reference")
public class MessageReference {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String messageId;

    private String docId;

    private String docTitle;

    private String content;

    private BigDecimal relevanceScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}