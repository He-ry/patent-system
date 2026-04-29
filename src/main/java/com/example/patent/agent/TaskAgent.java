package com.example.patent.agent;

import com.example.patent.agent.domain.AgentTaskMode;
import com.example.patent.agent.domain.AgentTaskPlan;
import com.example.patent.common.DatabaseSchema;
import com.example.patent.prompt.PromptTemplateService;
import com.example.patent.service.OpenAiService;
import com.example.patent.skill.SkillRegistry;
import com.example.patent.skill.domain.SkillRoutingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAgent {

    private final SkillRegistry skillRegistry;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;

    public SkillRoutingResult plan(String userQuery, String historyContext) {
        if (!skillRegistry.hasSkills()) {
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("No available tools")
                    .originalQuery(userQuery)
                    .build();
        }

        String normalized = userQuery == null ? "" : userQuery.toLowerCase(Locale.ROOT);
        if (isSimpleConversation(normalized)) {
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("Simple conversation")
                    .originalQuery(userQuery)
                    .build();
        }

        try {
            String systemPrompt = promptTemplateService.render(
                    "prompts/agent/task-planner-system.txt",
                    Map.of(
                            "skills", skillRegistry.getAllSkillContent(),
                            "schema", DatabaseSchema.FULL_SCHEMA,
                            "relation", DatabaseSchema.TABLE_RELATION
                    )
            );
            String userPrompt = promptTemplateService.render(
                    "prompts/agent/task-planner-user.txt",
                    Map.of(
                            "query", userQuery == null ? "" : userQuery,
                            "history", historyContext == null ? "" : historyContext
                    )
            );

            String response = openAiService.chatWithJson(systemPrompt, userPrompt);
            AgentTaskPlan plan = parsePlan(response);
            return toRoutingResult(plan, userQuery);
        } catch (Exception e) {
            log.error("Task agent planning failed: {}", e.getMessage());
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("Agent planning failed: " + e.getMessage())
                    .originalQuery(userQuery)
                    .build();
        }
    }

    private AgentTaskPlan parsePlan(String response) throws Exception {
        JsonNode node = objectMapper.readTree(extractJson(response));
        String modeText = node.path("mode").asText("CHAT");
        AgentTaskMode mode;
        try {
            mode = AgentTaskMode.valueOf(modeText.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            mode = AgentTaskMode.CHAT;
        }

        Map<String, Object> execution = null;
        JsonNode executionNode = node.path("execution");
        if (!executionNode.isMissingNode() && !executionNode.isNull()) {
            execution = objectMapper.convertValue(
                    executionNode,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
            );
        }

        return AgentTaskPlan.builder()
                .mode(mode)
                .needsTool(node.path("needsTool").asBoolean(false))
                .toolName(node.path("toolName").asText(""))
                .sql(node.path("sql").asText(""))
                .reason(node.path("reason").asText(""))
                .execution(execution)
                .build();
    }

    private SkillRoutingResult toRoutingResult(AgentTaskPlan plan, String userQuery) {
        if (plan == null || !plan.isNeedsTool() || plan.getMode() == AgentTaskMode.CHAT || plan.getMode() == AgentTaskMode.CLARIFY) {
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason(plan == null ? "No plan generated" : plan.getReason())
                    .originalQuery(userQuery)
                    .build();
        }

        String skillName = switch (plan.getMode()) {
            case QUERY -> "sql-generator";
            case ANALYSIS -> "data-analyzer";
            case REPORT -> "report-preview";
            default -> plan.getToolName();
        };

        return SkillRoutingResult.builder()
                .needsSkill(true)
                .skillName(skillName)
                .sql(plan.getSql())
                .reason(plan.getReason())
                .execution(plan.getExecution())
                .originalQuery(userQuery)
                .build();
    }

    private boolean isSimpleConversation(String normalized) {
        if (normalized == null || normalized.length() > 15) {
            return false;
        }
        return normalized.matches(".*(你好|您好|hi|hello|早上好|下午好|晚上好|谢谢|感谢|再见|拜拜|好的|ok|在吗).*");
    }

    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        return response.trim();
    }
}
