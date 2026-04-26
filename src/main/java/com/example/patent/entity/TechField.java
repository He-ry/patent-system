package com.example.patent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tech_field")
public class TechField {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patentId;
    private String fieldName;
    private Integer seq;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
