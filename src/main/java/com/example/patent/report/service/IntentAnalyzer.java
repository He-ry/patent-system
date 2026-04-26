package com.example.patent.report.service;

import com.example.patent.entity.ConversationMessage;
import com.example.patent.report.domain.ReportIntent;
import com.example.patent.service.OpenAiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentAnalyzer {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    /**
     * Analyze the user's report intent from the current query and conversation history.
     *
     * @param query   the current user input
     * @param history previous conversation messages (for context)
     * @return parsed ReportIntent with scope, filters, and dimensions
     */
    public ReportIntent analyze(String query, List<ConversationMessage> history) {
        if (!StringUtils.hasText(query)) {
            return defaultIntent(query);
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(query, history);

        try {
            String response = openAiService.chatWithJson(systemPrompt, userPrompt);
            Map<String, Object> parsed = objectMapper.readValue(response, new TypeReference<LinkedHashMap<String, Object>>() {});
            return validate(parseMap(parsed, query));
        } catch (Exception e) {
            log.warn("Intent analysis via AI failed, falling back to default: {}", e.getMessage());
            return defaultIntent(query);
        }
    }

    private String buildSystemPrompt() {
        return """
                你是专利报告意图分析器。你的任务是从用户输入中提取报告生成的结构化条件。
                只返回 JSON，不要解释。

                可用的 scope 类型：
                - comprehensive: 全库综合分析（用户没有指定具体对象）
                - inventor: 围绕单个发明人的报告
                - college: 围绕某个学院的报告
                - topic: 围绕某个技术主题的报告
                - custom: 自定义范围

                可用 focus（分析重点）：
                - trend: 趋势分析
                - value: 价值分析
                - technical_layout: 技术布局分析
                - legal_status: 法律状态分析
                - quality: 专利质量分析

                可用的 requestedDimensions（指定章节）：
                - basic-overview, annual-trend, annual-grant-trend
                - patent-type-dist, legal-status-dist
                - tech-field-dist, technical-subject-dist, application-field-dist
                - strategic-industry-dist, ipc-dist
                - college-ranking, inventor-ranking
                - patent-value-top, tech-value-top, market-value-top
                - cited-ranking, claims-ranking

                你必须结合对话上下文理解用户意图。
                例如：用户之前问了某发明人，现在说"出报告"，应推断为 inventor 范围。
                例如：用户说"当前知识库"或"全部数据"，应输出 comprehensive 且 keyword 为空。
                """;
    }

    private String buildUserPrompt(String query, List<ConversationMessage> history) {
        StringBuilder sb = new StringBuilder();

        // append conversation history for context
        if (history != null && !history.isEmpty()) {
            sb.append("以下是对话历史：\n");
            for (int i = Math.max(0, history.size() - 6); i < history.size(); i++) {
                ConversationMessage msg = history.get(i);
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                String content = msg.getContent();
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                sb.append(role).append(": ").append(content).append("\n\n");
            }
        }

        sb.append("用户当前请求：").append(query).append("\n\n");

        sb.append("请输出 JSON：\n");
        sb.append("{\n");
        sb.append("  \"titleHint\": \"报告标题（可选）\",\n");
        sb.append("  \"scope\": \"comprehensive|inventor|college|topic|custom\",\n");
        sb.append("  \"focus\": [\"trend\", \"value\", ...],\n");
        sb.append("  \"requestedDimensions\": [\"annual-trend\", \"patent-value-top\", ...],\n");
        sb.append("  \"filters\": {\n");
        sb.append("    \"inventor\": \"发明人姓名（可选）\",\n");
        sb.append("    \"college\": \"学院名称\",\n");
        sb.append("    \"patentType\": \"发明专利|实用新型|外观设计\",\n");
        sb.append("    \"legalStatus\": \"已授权|审查中|失效\",\n");
        sb.append("    \"assignee\": \"专利权人\",\n");
        sb.append("    \"keyword\": \"关键词\",\n");
        sb.append("    \"technicalField\": \"技术领域\",\n");
        sb.append("    \"ipcMainClassInterpretation\": \"IPC释义\",\n");
        sb.append("    \"applicationYearStart\": null,\n");
        sb.append("    \"applicationYearEnd\": null,\n");
        sb.append("    \"grantYearStart\": null,\n");
        sb.append("    \"grantYearEnd\": null\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        sb.append("要求：\n");
        sb.append("1. 结合对话历史理解上下文，不要只看当前请求\n");
        sb.append("2. 当前知识库、全部数据等应理解为 scope=comprehensive，不设 keyword\n");
        sb.append("3. 如果用户说的对象在数据库中存在值才设置过滤条件\n");
        sb.append("4. 没有明确提到的字段设为 null 或空\n");

        return sb.toString();
    }

    private ReportIntent parseMap(Map<String, Object> map, String rawQuery) {
        ReportIntent intent = new ReportIntent();
        intent.setRawQuery(rawQuery);
        intent.setTitleHint(asText(map.get("titleHint")));
        intent.setScope(normalizeScope(asText(map.get("scope"))));
        intent.setFocus(toStringList(map.get("focus")));
        intent.setRequestedDimensions(toStringList(map.get("requestedDimensions")));
        intent.setFilters(parseFilters(map.get("filters")));
        return intent;
    }

    private ReportIntent validate(ReportIntent intent) {
        if (intent.getFilters() == null) {
            intent.setFilters(new ReportIntent.Filters());
        }
        if (!StringUtils.hasText(intent.getScope())) {
            intent.setScope("comprehensive");
        }
        return intent;
    }

    private ReportIntent defaultIntent(String rawQuery) {
        ReportIntent intent = new ReportIntent();
        intent.setRawQuery(rawQuery);
        intent.setScope("comprehensive");
        intent.setFilters(new ReportIntent.Filters());
        intent.setFocus(new ArrayList<>());
        intent.setRequestedDimensions(new ArrayList<>());
        return intent;
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) return null;
        String s = scope.trim().toLowerCase(Locale.ROOT);
        if (Set.of("inventor", "college", "topic", "comprehensive", "custom").contains(s)) {
            return s;
        }
        return "custom";
    }

    private ReportIntent.Filters parseFilters(Object filtersValue) {
        if (!(filtersValue instanceof Map<?, ?> filtersMap)) {
            return new ReportIntent.Filters();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) filtersMap;
        ReportIntent.Filters filters = new ReportIntent.Filters();
        filters.setInventor(asText(map.get("inventor")));
        filters.setCollege(asText(map.get("college")));
        filters.setPatentType(asText(map.get("patentType")));
        filters.setLegalStatus(asText(map.get("legalStatus")));
        filters.setAssignee(asText(map.get("assignee")));
        filters.setKeyword(asText(map.get("keyword")));
        filters.setTechnicalField(asText(map.get("technicalField")));
        filters.setIpcMainClassInterpretation(asText(map.get("ipcMainClassInterpretation")));
        filters.setApplicationYearStart(asInteger(map.get("applicationYearStart")));
        filters.setApplicationYearEnd(asInteger(map.get("applicationYearEnd")));
        filters.setGrantYearStart(asInteger(map.get("grantYearStart")));
        filters.setGrantYearEnd(asInteger(map.get("grantYearEnd")));
        return filters;
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::asText)
                    .filter(StringUtils::hasText)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return new ArrayList<>();
    }

    private String asText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value != null) {
            try { return Integer.parseInt(String.valueOf(value).trim()); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
