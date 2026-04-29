package com.example.patent.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskPlan {
    private AgentTaskMode mode;
    private boolean needsTool;
    private String toolName;
    private String sql;
    private String reason;
    private Map<String, Object> execution;
}
