package com.example.patent.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GraphEdge {
    private String id;
    private String source;
    private String target;
    private String type;
    private Boolean directed;
    private Double weight;
    private Map<String, Object> properties;
}
