package com.example.patent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ipc_classification")
public class IpcClassification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patentId;
    private String ipcCode;
    private Integer seq;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
