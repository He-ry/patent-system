package com.example.patent.skill.persistence;

import com.example.patent.skill.domain.SkillDefinition;
import java.util.List;
import java.util.Optional;

public interface SkillPersistence {
    List<SkillDefinition> loadAll();
    Optional<SkillDefinition> load(String name);
    String getSkillPath();
}
