package com.example.patent.skill.domain;

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
public class SkillExecutionResult {
    private boolean success;
    private String skillName;
    private String sql;
    private String content;
    private List<Map<String, Object>> data;
    private Map<String, Object> statistics;
    private String error;
    private String reason;
    private long executionTime;
    private String chartPath;
    private String chartType;
    private Integer wordCount;
}
