package com.example.patent.skill;

import com.example.patent.skill.domain.SkillDefinition;
import com.example.patent.skill.persistence.SkillPersistence;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillRegistry {
    private final SkillPersistence persistence;

    @Getter
    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    @Getter
    private final List<SkillDefinition> enabledSkills = new ArrayList<>();

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        skills.clear();
        enabledSkills.clear();

        List<SkillDefinition> loaded = persistence.loadAll();
        for (SkillDefinition skill : loaded) {
            register(skill);
        }

        log.info("=== SkillRegistry 初始化完成 ===");
        log.info("已加载 {} 个Skills", skills.size());
        for (SkillDefinition skill : enabledSkills) {
            log.info("  - {} : {}", skill.getName(), skill.getDescription());
        }
    }

    public void register(SkillDefinition skill) {
        if (skill == null || skill.getName() == null) {
            log.warn("尝试注册无效的Skill");
            return;
        }
        skills.put(skill.getName(), skill);
        if (skill.isEnabled()) {
            if (!enabledSkills.contains(skill)) {
                enabledSkills.add(skill);
            }
        }
        log.info("注册Skill: {} (描述: {})", skill.getName(), skill.getDescription());
    }

    public SkillDefinition getSkill(String name) {
        return skills.get(name);
    }

    public String getAllSkillContent() {
        return enabledSkills.stream()
                .map(skill -> "## Skill: " + skill.getName() + "\n\n" + skill.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    public String getSkillContent(String name) {
        SkillDefinition skill = skills.get(name);
        return skill != null ? skill.getContent() : null;
    }

    public boolean hasSkills() {
        return !enabledSkills.isEmpty();
    }
}
