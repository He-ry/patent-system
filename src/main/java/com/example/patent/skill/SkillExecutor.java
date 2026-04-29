package com.example.patent.skill;

import com.example.patent.common.DatabaseSchema;
import com.example.patent.report.service.ReportOrchestrator;
import com.example.patent.service.OpenAiService;
import com.example.patent.skill.domain.SkillExecutionResult;
import com.example.patent.skill.domain.SkillRoutingResult;
import com.example.patent.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SkillExecutor {
    private static final Logger log = LoggerFactory.getLogger(SkillExecutor.class);

    private static final int SQL_QUERY_LIMIT = 100;
    private static final int ANALYSIS_QUERY_LIMIT = 50;
    private static final int RESULT_PREVIEW_ROWS = 100;
    private static final String SAFE_SQL_ERROR_MESSAGE = "数据查询暂时无法完成，请稍后重试或调整查询条件。";
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5A-Za-z0-9/_.-]{2,32}");
    private static final Pattern EVIDENCE_KEYWORD_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9/_.-]{1,31}|[\\u4e00-\\u9fa5]{2,8}");
    private static final Pattern LIMIT_OFFSET_PATTERN = Pattern.compile("(?is)\\s+limit\\s+(\\d+)\\s+offset\\s+(\\d+)\\s*;?\\s*$");
    private static final Pattern LIMIT_COMMA_PATTERN = Pattern.compile("(?is)\\s+limit\\s+(\\d+)\\s*,\\s*(\\d+)\\s*;?\\s*$");
    private static final Pattern SIMPLE_LIMIT_PATTERN = Pattern.compile("(?is)\\s+limit\\s+(\\d+)\\s*;?\\s*$");
    private static final Set<String> QUERY_STOPWORDS = Set.of(
            "查询", "查找", "找出", "列举", "显示", "展示", "所有", "全部", "相关",
            "数据", "专利", "信息", "内容", "结果", "多少", "哪些", "哪个", "什么",
            "为", "是", "和", "或", "的", "按", "根据", "里面", "数据库", "字段", "分类号",
            "继续", "下一页", "下页", "更多", "往下", "接着", "再来"
    );

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiService openAiService;
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
        log.info("执行 Skill: {}", skillName);

        return switch (skillName) {
            case "sql-generator" -> executeSql(routingResult, start, progressCallback);
            case "data-analyzer" -> executeDataAnalyzer(routingResult, start, progressCallback);
            case "report-preview" -> reportOrchestrator.execute(routingResult, start, progressCallback);
            default -> SkillExecutionResult.builder()
                    .success(false)
                    .error("暂时无法处理该类型的任务，请换一种问法重试。")
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        };
    }

    private void sendSkillStatus(Consumer<ChatEventVO> callback, String skillName, String status) {
        if (callback != null) {
            callback.accept(ChatEventVO.skill(skillName, status));
        }
    }

    private SkillExecutionResult executeSql(SkillRoutingResult routingResult, long start, Consumer<ChatEventVO> progressCallback) {
        String sql = safeTrim(routingResult.getSql());
        String userQuery = routingResult.getOriginalQuery();

        if (sql == null) {
            return SkillExecutionResult.builder()
                    .success(false)
                    .reason("SQL 为空")
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        }

        try {
            sendSkillStatus(progressCallback, "sql-generator", "正在验证 SQL 语法...");
            sql = applySelectLimit(sql, SQL_QUERY_LIMIT);
            validateSql(sql);

            sendSkillStatus(progressCallback, "sql-generator", "正在查询数据库...");
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            if (data.isEmpty()) {
                SkillExecutionResult recovered = retryForEmptyResult("sql-generator", sql, userQuery, start, progressCallback);
                if (recovered != null) {
                    return recovered;
                }
            } else if (resultMissesExplicitKeywords(userQuery, data)) {
                SkillExecutionResult recovered = retryForUnsatisfiedResult("sql-generator", sql, userQuery, start, progressCallback);
                if (recovered != null) {
                    return recovered;
                }
            }

            return SkillExecutionResult.builder()
                    .success(true)
                    .skillName("sql-generator")
                    .sql(sql)
                    .data(data)
                    .content(formatResult(data))
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("SQL 执行失败: {}", e.getMessage());
            sendSkillStatus(progressCallback, "sql-generator", "SQL 执行失败，正在尝试自动修复...");

            String fixedSql = rewriteSqlWithAi(sql, "SQL 执行报错: " + e.getMessage(), userQuery, progressCallback, "");
            if (fixedSql != null && !fixedSql.equalsIgnoreCase(sql)) {
                try {
                    fixedSql = applySelectLimit(fixedSql, SQL_QUERY_LIMIT);
                    validateSql(fixedSql);
                    sendSkillStatus(progressCallback, "sql-generator", "修复成功，正在重新执行查询...");
                    List<Map<String, Object>> data = jdbcTemplate.queryForList(fixedSql);
                    if (data.isEmpty()) {
                        SkillExecutionResult recovered = retryForEmptyResult("sql-generator", fixedSql, userQuery, start, progressCallback);
                        if (recovered != null) {
                            return recovered;
                        }
                    } else if (resultMissesExplicitKeywords(userQuery, data)) {
                        SkillExecutionResult recovered = retryForUnsatisfiedResult("sql-generator", fixedSql, userQuery, start, progressCallback);
                        if (recovered != null) {
                            return recovered;
                        }
                    }
                    return SkillExecutionResult.builder()
                            .success(true)
                            .skillName("sql-generator")
                            .sql(fixedSql)
                            .data(data)
                            .content(formatResult(data))
                            .executionTime(System.currentTimeMillis() - start)
                            .build();
                } catch (Exception retryError) {
                    log.error("修复后的 SQL 仍然失败: {}", retryError.getMessage());
                }
            }

            return SkillExecutionResult.builder()
                    .success(false)
                    .skillName("sql-generator")
                    .sql(sql)
                    .error(SAFE_SQL_ERROR_MESSAGE)
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private SkillExecutionResult executeDataAnalyzer(SkillRoutingResult routingResult, long start, Consumer<ChatEventVO> progressCallback) {
        String userQuery = routingResult.getOriginalQuery();
        sendSkillStatus(progressCallback, "data-analyzer", "正在准备数据查询...");

        try {
            Map<String, Object> execution = routingResult.getExecution();
            String sql = execution == null ? null : safeTrim((String) execution.get("sql"));
            if (sql == null) {
                sql = safeTrim(routingResult.getSql());
            }

            if (sql == null) {
                return SkillExecutionResult.builder()
                        .success(false)
                        .skillName("data-analyzer")
                        .error(SAFE_SQL_ERROR_MESSAGE)
                        .executionTime(System.currentTimeMillis() - start)
                        .build();
            }

            sql = adjustSqlLimit(sql, "data-analyzer");
            sendSkillStatus(progressCallback, "data-analyzer", "正在验证 SQL 语法...");
            validateSql(sql);

            sendSkillStatus(progressCallback, "data-analyzer", "正在执行数据查询和统计...");
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            if (data.isEmpty()) {
                SkillExecutionResult recovered = retryForEmptyResult("data-analyzer", sql, userQuery, start, progressCallback);
                if (recovered != null) {
                    return recovered;
                }
            } else if (resultMissesExplicitKeywords(userQuery, data)) {
                SkillExecutionResult recovered = retryForUnsatisfiedResult("data-analyzer", sql, userQuery, start, progressCallback);
                if (recovered != null) {
                    return recovered;
                }
            }

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
            log.error("data-analyzer 执行失败: {}", e.getMessage());
            sendSkillStatus(progressCallback, "data-analyzer", "查询失败，正在尝试自动修复...");

            String fixedSql = rewriteSqlWithAi(safeTrim(routingResult.getSql()), "SQL 执行报错: " + e.getMessage(), userQuery, progressCallback, "");
            if (fixedSql != null && !fixedSql.equalsIgnoreCase(safeTrim(routingResult.getSql()))) {
                try {
                    fixedSql = adjustSqlLimit(fixedSql, "data-analyzer");
                    validateSql(fixedSql);
                    sendSkillStatus(progressCallback, "data-analyzer", "修复成功，正在重新执行查询...");
                    List<Map<String, Object>> data = jdbcTemplate.queryForList(fixedSql);
                    if (data.isEmpty()) {
                        SkillExecutionResult recovered = retryForEmptyResult("data-analyzer", fixedSql, userQuery, start, progressCallback);
                        if (recovered != null) {
                            return recovered;
                        }
                    } else if (resultMissesExplicitKeywords(userQuery, data)) {
                        SkillExecutionResult recovered = retryForUnsatisfiedResult("data-analyzer", fixedSql, userQuery, start, progressCallback);
                        if (recovered != null) {
                            return recovered;
                        }
                    }
                    Map<String, Object> statistics = calculateDataStatistics(data);
                    return SkillExecutionResult.builder()
                            .success(true)
                            .skillName("data-analyzer")
                            .sql(fixedSql)
                            .data(data)
                            .statistics(statistics)
                            .content(formatDataAnalysisResult(data, statistics))
                            .executionTime(System.currentTimeMillis() - start)
                            .build();
                } catch (Exception retryError) {
                    log.error("修复后的 SQL 仍然失败: {}", retryError.getMessage());
                }
            }

            return SkillExecutionResult.builder()
                    .success(false)
                    .skillName("data-analyzer")
                    .error(SAFE_SQL_ERROR_MESSAGE)
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private SkillExecutionResult retryForEmptyResult(String skillName,
                                                     String sql,
                                                     String userQuery,
                                                     long start,
                                                     Consumer<ChatEventVO> progressCallback) {
        sendSkillStatus(progressCallback, skillName, "首轮查询为空，正在观察数据库样本并自动重试...");

        String sampleContext = buildObservationContext(userQuery, sql);
        String rewrittenSql = rewriteSqlWithAi(
                sql,
                "SQL 能执行，但返回 0 条结果。请根据数据库样本重新判断应该查询的表、字段、JOIN 和匹配方式。",
                userQuery,
                progressCallback,
                sampleContext
        );

        if (rewrittenSql == null || rewrittenSql.equalsIgnoreCase(sql)) {
            return null;
        }

        try {
            String retrySql = "data-analyzer".equals(skillName)
                    ? adjustSqlLimit(rewrittenSql, skillName)
                    : applySelectLimit(rewrittenSql, SQL_QUERY_LIMIT);
            validateSql(retrySql);
            sendSkillStatus(progressCallback, skillName, "已根据样本重写查询，正在再次执行...");
            List<Map<String, Object>> data = jdbcTemplate.queryForList(retrySql);
            if (data.isEmpty()) {
                return null;
            }

            if ("data-analyzer".equals(skillName)) {
                Map<String, Object> statistics = calculateDataStatistics(data);
                return SkillExecutionResult.builder()
                        .success(true)
                        .skillName(skillName)
                        .sql(retrySql)
                        .data(data)
                        .statistics(statistics)
                        .content(formatDataAnalysisResult(data, statistics))
                        .executionTime(System.currentTimeMillis() - start)
                        .build();
            }

            return SkillExecutionResult.builder()
                    .success(true)
                    .skillName(skillName)
                    .sql(retrySql)
                    .data(data)
                    .content(formatResult(data) + buildMatchEvidenceNote(userQuery, retrySql))
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception retryError) {
            log.warn("Empty-result retry failed: {}", retryError.getMessage());
            return null;
        }
    }

    private SkillExecutionResult retryForUnsatisfiedResult(String skillName,
                                                           String sql,
                                                           String userQuery,
                                                           long start,
                                                           Consumer<ChatEventVO> progressCallback) {
        sendSkillStatus(progressCallback, skillName, "查询结果与明确检索值不匹配，正在重新观察数据库...");

        String sampleContext = buildObservationContext(userQuery, sql);
        String rewrittenSql = rewriteSqlWithAi(
                sql,
                "SQL 返回了数据，但结果内容没有包含用户给出的明确检索值。请重新判断真实字段、JOIN 和匹配方式，并把匹配到的证据字段一并 SELECT 出来。",
                userQuery,
                progressCallback,
                sampleContext
        );

        if (rewrittenSql == null || rewrittenSql.equalsIgnoreCase(sql)) {
            return null;
        }

        try {
            String retrySql = "data-analyzer".equals(skillName)
                    ? adjustSqlLimit(rewrittenSql, skillName)
                    : applySelectLimit(rewrittenSql, SQL_QUERY_LIMIT);
            validateSql(retrySql);
            sendSkillStatus(progressCallback, skillName, "已根据匹配证据重写查询，正在再次执行...");
            List<Map<String, Object>> data = jdbcTemplate.queryForList(retrySql);
            if (data.isEmpty()) {
                return null;
            }
            String evidenceNote = buildMatchEvidenceNote(userQuery, retrySql);

            if ("data-analyzer".equals(skillName)) {
                Map<String, Object> statistics = calculateDataStatistics(data);
                return SkillExecutionResult.builder()
                        .success(true)
                        .skillName(skillName)
                        .sql(retrySql)
                        .data(data)
                        .statistics(statistics)
                        .content(formatDataAnalysisResult(data, statistics) + evidenceNote)
                        .executionTime(System.currentTimeMillis() - start)
                        .build();
            }

            return SkillExecutionResult.builder()
                    .success(true)
                    .skillName(skillName)
                    .sql(retrySql)
                    .data(data)
                    .content(formatResult(data) + evidenceNote)
                    .executionTime(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception retryError) {
            log.warn("Unsatisfied-result retry failed: {}", retryError.getMessage());
            return null;
        }
    }

    private boolean resultMissesExplicitKeywords(String userQuery, List<Map<String, Object>> rows) {
        List<String> keywords = extractEvidenceKeywords(userQuery);
        if (keywords.isEmpty() || rows == null || rows.isEmpty()) {
            return false;
        }

        String resultText = rows.stream()
                .flatMap(row -> row.values().stream())
                .map(value -> value == null ? "" : String.valueOf(value))
                .reduce("", (left, right) -> left + "\n" + right)
                .toLowerCase();

        for (String keyword : keywords) {
            if (resultText.contains(keyword.toLowerCase())) {
                return false;
            }
        }
        log.info("Result did not contain explicit query keywords: {}", keywords);
        return true;
    }

    private List<String> extractEvidenceKeywords(String userQuery) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (userQuery == null || userQuery.isBlank()) {
            return new ArrayList<>();
        }

        Matcher matcher = EVIDENCE_KEYWORD_PATTERN.matcher(userQuery);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() < 2 || QUERY_STOPWORDS.contains(token)) {
                continue;
            }
            boolean codeLike = token.matches(".*[0-9/_.-].*");
            boolean nameLike = token.matches("[\\u4e00-\\u9fa5]{2,4}");
            if (codeLike || nameLike) {
                keywords.add(token);
            }
            if (keywords.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    private String buildMatchEvidenceNote(String userQuery, String sql) {
        List<String> keywords = extractEvidenceKeywords(userQuery);
        if (keywords.isEmpty() || sql == null) {
            return "";
        }

        List<String> matchedKeywords = keywords.stream()
                .filter(keyword -> sql.toLowerCase().contains(keyword.toLowerCase()))
                .toList();
        if (matchedKeywords.isEmpty()) {
            return "";
        }

        return "\n\n匹配证据：本次 SQL 已按用户明确检索值过滤：" + String.join(", ", matchedKeywords)
                + "。如果结果主表字段没有直接展示该值，匹配值可能位于扩展字段或 JOIN 条件中，请不要仅根据主表字段否定查询结果。";
    }

    private String buildObservationContext(String userQuery, String currentSql) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库样本观察：\n");
        sb.append("## 当前 SQL\n").append(currentSql).append("\n");

        appendSample(sb, "patent_info 常用字段样本",
                """
                SELECT id, title, application_number, patent_type, legal_status, college, ipc_main_class
                FROM patent_info
                WHERE deleted = 0
                LIMIT 5
                """);

        appendSample(sb, "patent_info_field 字段类型分布",
                """
                SELECT field_type, COUNT(*) AS cnt
                FROM patent_info_field
                GROUP BY field_type
                ORDER BY cnt DESC
                LIMIT 20
                """);

        appendSample(sb, "patent_info_field 样本",
                """
                SELECT field_type, field_value
                FROM patent_info_field
                LIMIT 20
                """);

        for (String keyword : extractQueryKeywords(userQuery)) {
            appendSample(sb, "关键词命中样本: " + keyword,
                    buildKeywordProbeSql(keyword));
        }

        return sb.toString();
    }

    private List<String> extractQueryKeywords(String userQuery) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (userQuery == null || userQuery.isBlank()) {
            return new ArrayList<>();
        }

        Matcher matcher = KEYWORD_PATTERN.matcher(userQuery);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() < 2) {
                continue;
            }
            if (QUERY_STOPWORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
            if (keywords.size() >= 5) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    private String buildKeywordProbeSql(String keyword) {
        String escaped = escapeSql(keyword);
        List<Map<String, Object>> columns = searchableTextColumns();
        StringBuilder sql = new StringBuilder();

        for (Map<String, Object> column : columns) {
            String tableName = String.valueOf(column.get("table_name"));
            String columnName = String.valueOf(column.get("column_name"));
            if (!isSafeIdentifier(tableName) || !isSafeIdentifier(columnName)) {
                continue;
            }

            if (!sql.isEmpty()) {
                sql.append("\nUNION ALL\n");
            }
            sql.append("(SELECT '")
                    .append(tableName)
                    .append(".")
                    .append(columnName)
                    .append("' AS source, `")
                    .append(columnName)
                    .append("` AS value FROM `")
                    .append(tableName)
                    .append("` WHERE `")
                    .append(columnName)
                    .append("` LIKE '%")
                    .append(escaped)
                    .append("%' LIMIT 3)");
        }

        if (sql.isEmpty()) {
            return "SELECT 'no_searchable_text_columns' AS source, NULL AS value WHERE 1 = 0";
        }
        return sql.append("\nLIMIT 12").toString();
    }

    private List<Map<String, Object>> searchableTextColumns() {
        return jdbcTemplate.queryForList("""
                SELECT TABLE_NAME AS table_name, COLUMN_NAME AS column_name
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME IN ('patent_info', 'patent_info_field')
                  AND DATA_TYPE IN ('char', 'varchar', 'text', 'mediumtext', 'longtext')
                ORDER BY TABLE_NAME, ORDINAL_POSITION
                """);
    }

    private boolean isSafeIdentifier(String value) {
        return value != null && value.matches("[A-Za-z0-9_]+");
    }

    private void appendSample(StringBuilder sb, String title, String sql) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            sb.append("## ").append(title).append("\n").append(formatRows(rows, 10)).append("\n");
        } catch (Exception e) {
            sb.append("## ").append(title).append("\n样本读取失败: ").append(e.getMessage()).append("\n");
        }
    }

    private String formatRows(List<Map<String, Object>> rows, int limit) {
        if (rows == null || rows.isEmpty()) {
            return "无样本";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(limit, rows.size()); i++) {
            Map<String, Object> row = rows.get(i);
            boolean first = true;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (!first) {
                    sb.append(" | ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String rewriteSqlWithAi(String sql,
                                    String issue,
                                    String userQuery,
                                    Consumer<ChatEventVO> progressCallback,
                                    String observationContext) {
        try {
            sendSkillStatus(progressCallback, "sql-generator", "正在根据结构和样本重写 SQL...");
            String prompt = """
                    你是专业 SQL 工程师，需要改写一条查询 SQL。

                    用户原始问题：
                    %s

                    数据库表结构：
                    %s

                    %s

                    当前 SQL：
                    ```sql
                    %s
                    ```

                    当前问题：
                    %s

                    %s

                    要求：
                    1. 不要只根据表结构想象字段，必须结合样本观察判断真实存储位置
                    2. 如果目标值不在主表，而在扩展表 patent_info_field，要自行补充 JOIN
                    3. 如果样本显示字段值可能是多值拼接，优先使用 LIKE，而不是 =
                    4. 保留 deleted = 0 等有效过滤
                    5. 只返回一条可执行的 SELECT SQL，不要解释，不要 markdown
                    """.formatted(
                    userQuery == null ? "" : userQuery,
                    DatabaseSchema.PATENT_INFO_TABLE,
                    DatabaseSchema.PATENT_INFO_FIELD_TABLE + "\n" + DatabaseSchema.TABLE_RELATION,
                    sql,
                    issue,
                    observationContext == null ? "" : observationContext
            );

            String rewrittenSql = openAiService.chat(
                    "你是专业 SQL 工程师，擅长根据真实样本修正查错字段、查错表、JOIN 缺失和匹配方式不当的问题。",
                    prompt
            );
            rewrittenSql = normalizeSqlResponse(rewrittenSql);
            log.info("[SQL Rewrite] original={}, rewritten={}", sql, rewrittenSql);
            return rewrittenSql;
        } catch (Exception e) {
            log.warn("AI SQL rewrite failed: {}", e.getMessage());
            return null;
        }
    }

    private String adjustSqlLimit(String sql, String sectionTitle) {
        return applySelectLimit(sql, determineLimit(sectionTitle));
    }

    private int determineLimit(String sectionTitle) {
        return "data-analyzer".equals(sectionTitle) ? ANALYSIS_QUERY_LIMIT : SQL_QUERY_LIMIT;
    }

    private String applySelectLimit(String sql, int maxLimit) {
        String normalized = sql == null ? "" : sql.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        Matcher limitOffsetMatcher = LIMIT_OFFSET_PATTERN.matcher(normalized);
        if (limitOffsetMatcher.find()) {
            int currentLimit;
            try {
                currentLimit = Integer.parseInt(limitOffsetMatcher.group(1));
            } catch (NumberFormatException e) {
                return normalized;
            }
            if (currentLimit <= maxLimit) {
                return normalized;
            }
            return normalized.substring(0, limitOffsetMatcher.start()).trim()
                    + " LIMIT " + maxLimit + " OFFSET " + limitOffsetMatcher.group(2);
        }

        Matcher limitCommaMatcher = LIMIT_COMMA_PATTERN.matcher(normalized);
        if (limitCommaMatcher.find()) {
            int currentLimit;
            try {
                currentLimit = Integer.parseInt(limitCommaMatcher.group(2));
            } catch (NumberFormatException e) {
                return normalized;
            }
            if (currentLimit <= maxLimit) {
                return normalized;
            }
            return normalized.substring(0, limitCommaMatcher.start()).trim()
                    + " LIMIT " + limitCommaMatcher.group(1) + ", " + maxLimit;
        }

        Matcher matcher = SIMPLE_LIMIT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            int currentLimit;
            try {
                currentLimit = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return normalized;
            }
            if (currentLimit <= maxLimit) {
                return normalized;
            }
            return normalized.substring(0, matcher.start()).trim() + " LIMIT " + maxLimit;
        }

        return normalized + " LIMIT " + maxLimit;
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL 语句为空");
        }

        String upperSql = sql.toUpperCase();
        if (!upperSql.trim().startsWith("SELECT")) {
            throw new IllegalArgumentException("只允许执行 SELECT 查询");
        }

        String[] dangerous = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE", "CREATE"};
        for (String keyword : dangerous) {
            if (upperSql.contains(keyword + " ")) {
                throw new IllegalArgumentException("禁止执行危险 SQL 操作: " + keyword);
            }
        }
    }

    private String formatResult(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "未查询到相关数据。";
        }

        StringBuilder sb = new StringBuilder();
        int rowLimit = Math.min(RESULT_PREVIEW_ROWS, data.size());
        sb.append("查询结果（本页 ").append(data.size()).append(" 条，展示前 ")
                .append(rowLimit).append(" 条）：\n\n");
        if (data.size() >= SQL_QUERY_LIMIT) {
            sb.append("说明：当前请求可能命中大量数据，系统已按分页预览模式返回第一页。")
                    .append("如需继续查看，请询问“下一页”或使用导出功能。\n\n");
        }
        for (int i = 0; i < rowLimit; i++) {
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
                    if (!first) {
                        sb.append(" | ");
                    }
                    sb.append(translateFieldName(entry.getKey())).append(": ").append(formatValue(entry.getValue()));
                    first = false;
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String translateFieldName(String fieldName) {
        if (fieldName == null) {
            return "";
        }
        return switch (fieldName) {
            case "college" -> "学院";
            case "patent_type" -> "专利类型";
            case "legal_status" -> "法律状态";
            case "application_year" -> "申请年";
            case "patent_value" -> "专利价值";
            case "cited_patents" -> "被引次数";
            case "cited_in_5_years" -> "5年被引次数";
            case "ipc_main_class" -> "IPC 主分类";
            case "ipc_main_class_interpretation" -> "IPC 主分类释义";
            default -> fieldName;
        };
    }

    private Object formatValue(Object value) {
        return value == null ? "无" : value;
    }

    private String normalizeSqlResponse(String sql) {
        if (sql == null) {
            return null;
        }
        String normalized = sql.trim();
        if (normalized.startsWith("```")) {
            int start = normalized.indexOf('\n') + 1;
            int end = normalized.lastIndexOf("```");
            if (end > start) {
                normalized = normalized.substring(start, end).trim();
            }
        }
        return normalized;
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
