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
public class SkillRoutingResult {
    private boolean needsSkill;
    private String skillName;
    private String sql;
    private String reason;
    private String originalQuery;
    private Map<String, Object> execution;
    private String conversationId;
    private String messageId;
}
