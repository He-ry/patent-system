package com.example.patent.skill.persistence;

import com.example.patent.skill.domain.SkillDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FileSkillPersistence implements SkillPersistence {
    private final Path skillsPath;

    public FileSkillPersistence(@Value("${skill.persist.path:.trae/skills}") String path) {
        this.skillsPath = Paths.get(path);
    }

    @Override
    public String getSkillPath() {
        return skillsPath.toString();
    }

    @Override
    public List<SkillDefinition> loadAll() {
        List<SkillDefinition> skills = new ArrayList<>();
        if (!Files.exists(skillsPath)) {
            log.warn("Skills目录不存在: {}", skillsPath);
            return skills;
        }

        try {
            Files.list(skillsPath)
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        Path skillFile = dir.resolve("SKILL.md");
                        if (Files.exists(skillFile)) {
                            try {
                                load(dir.getFileName().toString()).ifPresent(skills::add);
                            } catch (Exception e) {
                                log.error("加载Skill失败: {}", dir.getFileName(), e);
                            }
                        }
                    });
        } catch (IOException e) {
            log.error("扫描Skills目录失败", e);
        }

        log.info("加载了 {} 个Skills", skills.size());
        return skills;
    }

    @Override
    public Optional<SkillDefinition> load(String name) {
        Path skillFile = skillsPath.resolve(name).resolve("SKILL.md");
        if (!Files.exists(skillFile)) {
            log.warn("Skill文件不存在: {}", skillFile);
            return Optional.empty();
        }

        try {
            String content = Files.readString(skillFile);
            return Optional.of(parseSkillMarkdown(name, content));
        } catch (IOException e) {
            log.error("读取Skill文件失败: {}", skillFile, e);
            return Optional.empty();
        }
    }

    private SkillDefinition parseSkillMarkdown(String name, String content) {
        SkillDefinition.SkillDefinitionBuilder builder = SkillDefinition.builder();
        builder.name(name);
        builder.content(content);
        builder.enabled(true);

        String[] parts = content.split("---", 3);
        if (parts.length >= 2) {
            String yamlFront = parts[1].trim();
            try {
                Yaml yaml = new Yaml();
                Map<String, Object> frontMatter = yaml.load(yamlFront);
                builder.description((String) frontMatter.getOrDefault("description", ""));
            } catch (Exception e) {
                builder.description(extractDescription(content));
            }
        } else {
            builder.description(extractDescription(content));
        }

        return builder.build();
    }

    private String extractDescription(String content) {
        Pattern pattern = Pattern.compile("^#\\s+.+$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group().replace("# ", "").trim();
        }
        return content.substring(0, Math.min(100, content.length()));
    }
}
