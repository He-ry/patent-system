package com.example.patent.skill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDefinition {
    private String name;
    private String description;
    private String content;
    private boolean enabled;
}
