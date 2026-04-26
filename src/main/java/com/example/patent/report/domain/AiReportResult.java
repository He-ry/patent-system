package com.example.patent.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportResult {
    private String title;
    private String executiveSummary;
    private String conclusionSummary;
    private List<String> keyFindings;
    private List<ReportSection> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportSection {
        private String title;
        private String objective;
        private String analysisMarkdown;
        private Map<String, Object> chartOption;
        private String chartType;
        private List<Map<String, Object>> data;
        private List<String> keyFindings;
    }
}
