package com.example.patent.report.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportInfoVO {
    private String reportId;
    private String title;
    private String reportType;
    private Integer sectionCount;
    private Integer chartCount;
    private Integer totalWords;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
