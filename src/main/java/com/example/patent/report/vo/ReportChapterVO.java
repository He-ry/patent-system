package com.example.patent.report.vo;

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
public class ReportChapterVO {
    private String id;
    private String title;
    private String objective;
    private List<Map<String, Object>> data;
    private String base64Png;
    private String analysisMarkdown;
    private Map<String, Object> chartOption;
    private List<String> keyFindings;
}
