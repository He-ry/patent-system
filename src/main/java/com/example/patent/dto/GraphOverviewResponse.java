package com.example.patent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GraphOverviewResponse {
    private Map<String, Long> nodeCounts;
    private Map<String, Long> relationshipCounts;
    private List<Map<String, Object>> topInventors;
    private List<Map<String, Object>> topTopics;
    private List<Map<String, Object>> topIndustries;
}
