package com.example.patent.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("report_record")
public class ReportRecord {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String conversationId;
    private String messageId;
    private String title;
    private String reportType;
    private String htmlPath;
    private String docxPath;
    private String pdfPath;
    private String executiveSummary;
    private Integer sectionCount;
    private Integer chartCount;
    private Integer totalWords;
    private String status;
    private LocalDateTime createdAt;
}
