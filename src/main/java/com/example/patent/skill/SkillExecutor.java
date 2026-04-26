package com.example.patent.skill;

import com.example.patent.report.service.ReportOrchestrator;
import com.example.patent.service.OpenAiService;
import com.example.patent.skill.domain.SkillExecutionResult;
import com.example.patent.skill.domain.SkillRoutingResult;
import com.example.patent.vo.ChatEventVO;
import com.example.patent.common.SqlValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SkillExecutor {
    private static final Logger log = LoggerFactory.getLogger(SkillExecutor.class);

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final ReportOrchestrator reportOrchestrator;

    public SkillExecutionResult execute(SkillRoutingResult routingResult) {
        return execute(routingResult, null);
    }

    public SkillExecutionResult execute(SkillRoutingResult routingResult, Consumer<ChatEventVO> progressCallback) {
        long start = System.currentTimeMillis();
        if (routingResult == null || !routingResult.isNeedsSkill()) {
            return SkillExecutionResult.builder()
                    .success(false)
                    .reason("不需要执行技能")
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        }

        String skillName = routingResult.getSkillName();
        log.info("执行Skill: {}", skillName);

        return switch (skillName) {
            case "sql-generator" -> executeSql(routingResult, start, progressCallback);
            case "data-analyzer" -> executeDataAnalyzer(routingResult, start, progressCallback);
            case "report-preview" -> reportOrchestrator.execute(routingResult, start, progressCallback);
            default -> SkillExecutionResult.builder()
                    .success(false)
                    .error("未知的Skill: " + skillName)
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        };
    }


    private void sendStatus(Consumer<ChatEventVO> callback, String status) {
        if (callback != null) {
            callback.accept(ChatEventVO.status(status));
        }
    }

    private void sendStatus(Consumer<ChatEventVO> callback, String status, Integer progress) {
        if (callback != null) {
            callback.accept(ChatEventVO.status(status, progress));
        }
    }

    private void sendSkillStatus(Consumer<ChatEventVO> callback, String skillName, String status) {
        if (callback != null) {
            callback.accept(ChatEventVO.skill(skillName, status));
        }
    }

    private SkillExecutionResult executeSql(SkillRoutingResult routingResult, long start, Consumer<ChatEventVO> progressCallback) {
        String sql = routingResult.getSql();
        if (sql == null || sql.isBlank()) {
            return SkillExecutionResult.builder()
                    .success(false)
                    .reason("SQL为空")
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        }

        sql = sql.trim();
        sendSkillStatus(progressCallback, "sql-generator", "正在查询数据库...");

        try {
            validateSql(sql);
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            String content = formatResult(data);

            return SkillExecutionResult.builder()
                    .success(true)
                    .skillName("sql-generator")
                    .sql(sql)
                    .data(data)
                    .content(content)
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage());
            // One retry: send error + SQL to AI for correction
            String fixedSql = fixSqlWithAi(sql, e.getMessage());
            if (fixedSql != null && !fixedSql.equals(sql)) {
                try {
                    validateSql(fixedSql);
                    List<Map<String, Object>> data = jdbcTemplate.queryForList(fixedSql);
                    return SkillExecutionResult.builder()
                            .success(true)
                            .skillName("sql-generator")
                            .sql(fixedSql)
                            .data(data)
                            .content(formatResult(data))
                            .executionTime(System.currentTimeMillis() - start)
                            .build();
                } catch (Exception ignored) {}
            }
            return SkillExecutionResult.builder()
                    .success(false)
                    .skillName("sql-generator")
                    .sql(sql)
                    .error(e.getMessage())
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private SkillExecutionResult executeDataAnalyzer(SkillRoutingResult routingResult, long start, Consumer<ChatEventVO> progressCallback) {
        sendSkillStatus(progressCallback, "data-analyzer", "正在执行数据查询和统计...");

        try {
            Map<String, Object> execution = routingResult.getExecution();
            String sql = execution == null ? null : (String) execution.get("sql");
            if (sql == null || sql.isBlank()) {
                sql = routingResult.getSql();
            }

            if (sql == null || sql.isBlank()) {
                return SkillExecutionResult.builder()
                        .success(false)
                        .skillName("data-analyzer")
                        .error("SQL为空")
                        .executionTime(System.currentTimeMillis() - start)
                        .build();
            }

            sql = adjustSqlLimit(sql, "data-analyzer");
            validateSql(sql);

            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            Map<String, Object> statistics = calculateDataStatistics(data);

            return SkillExecutionResult.builder()
                    .success(true)
                    .skillName("data-analyzer")
                    .sql(sql)
                    .data(data)
                    .statistics(statistics)
                    .content(formatDataAnalysisResult(data, statistics))
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("data-analyzer执行失败: {}", e.getMessage());
            return SkillExecutionResult.builder()
                    .success(false)
                    .skillName("data-analyzer")
                    .error(e.getMessage())
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private String adjustSqlLimit(String sql, String sectionTitle) {
        String normalized = sql.trim();
        if (normalized.toLowerCase().contains(" limit ")) {
            return normalized;
        }
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + " LIMIT " + determineLimit(sectionTitle);
    }

    private int determineLimit(String sectionTitle) {
        return "data-analyzer".equals(sectionTitle) ? 50 : 30;
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL语句为空");
        }

        String upperSql = sql.toUpperCase();
        if (!upperSql.trim().startsWith("SELECT")) {
            throw new IllegalArgumentException("只允许执行SELECT查询");
        }

        String[] dangerous = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE", "CREATE"};
        for (String keyword : dangerous) {
            if (upperSql.contains(keyword + " ")) {
                throw new IllegalArgumentException("禁止执行危险SQL操作: " + keyword);
            }
        }
    }

    private String formatResult(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "未查询到相关数据。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询结果（共 ").append(data.size()).append(" 条）：\n\n");
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            sb.append("【第 ").append(i + 1).append(" 条】\n");
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                sb.append("  ").append(translateFieldName(entry.getKey())).append(": ")
                        .append(formatValue(entry.getValue())).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private Map<String, Object> calculateDataStatistics(List<Map<String, Object>> data) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("rowCount", data == null ? 0 : data.size());
        stats.put("columnCount", data == null || data.isEmpty() ? 0 : data.get(0).size());
        return stats;
    }

    private String formatDataAnalysisResult(List<Map<String, Object>> data, Map<String, Object> statistics) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 数据分析结果\n\n");
        sb.append("**数据总量**: ").append(statistics.getOrDefault("rowCount", 0)).append(" 条\n");
        sb.append("**字段数量**: ").append(statistics.getOrDefault("columnCount", 0)).append(" 个\n\n");

        if (data != null && !data.isEmpty()) {
            sb.append("## 数据预览\n\n");
            int limit = Math.min(5, data.size());
            for (int i = 0; i < limit; i++) {
                sb.append("**").append(i + 1).append(".** ");
                Map<String, Object> row = data.get(i);
                boolean first = true;
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (!first) sb.append(" | ");
                    sb.append(translateFieldName(entry.getKey())).append(": ").append(formatValue(entry.getValue()));
                    first = false;
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String translateFieldName(String fieldName) {
        if (fieldName == null) return "";
        return switch (fieldName) {
            case "college" -> "学院";
            case "patent_type" -> "专利类型";
            case "legal_status" -> "法律状态";
            case "application_year" -> "申请年";
            case "patent_value" -> "专利价值";
            case "cited_patents" -> "被引用数";
            case "cited_in_5_years" -> "5年引用数";
            case "ipc_main_class" -> "IPC分类";
            case "ipc_main_class_interpretation" -> "IPC分类释义";
            default -> fieldName;
        };
    }

    private Object formatValue(Object value) {
        return value == null ? "无" : value;
    }

    private String fixSqlWithAi(String sql, String error) {
        try {
            String prompt = "下面的 SQL 执行报错，请修复。只返回修复后的 SQL，不要解释。\n\n原 SQL：\n%s\n\n错误：\n%s".formatted(sql, error);
            return openAiService.chat("你是专业的 SQL 工程师，修复有语法错误的 SQL。", prompt);
        } catch (Exception e) {
            log.warn("AI 修复 SQL 失败: {}", e.getMessage());
            return null;
        }
    }
}
