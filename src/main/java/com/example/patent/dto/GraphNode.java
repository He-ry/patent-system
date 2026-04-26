package com.example.patent.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GraphNode {
    private String id;
    private String label;
    private String type;
    private Integer size;
    private Map<String, Object> properties;
}
