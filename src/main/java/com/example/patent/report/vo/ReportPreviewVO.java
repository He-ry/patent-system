package com.example.patent.report.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPreviewVO {
    private String title;
    private String generatedAt;
    private String executiveSummary;
    private String conclusionSummary;
    private List<String> keyFindings;
    private List<ReportChapterVO> chapters;
}
