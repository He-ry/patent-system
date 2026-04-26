package com.example.patent.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GraphDataResponse {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private Map<String, Object> summary;
}
