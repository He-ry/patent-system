
package com.example.patent.report.service;

import com.example.patent.common.SqlValidator;
import com.example.patent.entity.ConversationMessage;
import com.example.patent.report.domain.AiReportResult;
import com.example.patent.service.OpenAiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportGenerator {

    private static final String SCHEMA = """
            ## Database Schema
            
            ### patent_info (主表)
            | 字段名 | 类型 | 说明 |
            |---|---|---|
            | id | varchar(50) | 主键ID |
            | serial_number | varchar(50) | 序号 / 数据编号 |
            | title | varchar(255) | 专利标题 |
            | application_date | date | 申请日 |
            | inventor_count | int | 发明人数量 |
            | college | varchar(100) | 所属学院 / 单位 |
            | legal_status | varchar(20) | 法律状态 / 法律事件 |
            | patent_type | varchar(20) | 专利类型，例如：发明、实用新型、外观设计 |
            | application_number | varchar(50) | 申请号 |
            | grant_date | date | 授权日 |
            | current_assignee | varchar(255) | 当前申请人 / 当前专利权人 |
            | original_assignee | varchar(255) | 原始申请人 / 原始专利权人 |
            | agency | varchar(255) | 代理机构 |
            | current_assignee_province | varchar(50) | 当前申请人省份 / 州 |
            | original_assignee_province | varchar(50) | 原始申请人省份 / 州 |
            | original_assignee_type | varchar(50) | 原始申请人类型 |
            | current_assignee_type | varchar(50) | 当前申请人类型 |
            | application_year | int | 申请年份 |
            | publication_date | date | 公开 / 公告日 |
            | grant_year | int | 授权年份 |
            | ipc_main_class | varchar(50) | IPC主分类号 |
            | application_field_classification | varchar(255) | 应用领域分类 |
            | expiry_date | date | 失效日 |
            | simple_family_cited_patents | int | 简单同族被引用专利数量 |
            | cited_patents | int | 被引用专利数量 |
            | cited_in_5_years | int | 5年内被引用数量 |
            | claims_count | int | 权利要求数量 |
            | patent_value | decimal(15,2) | 专利价值 |
            | technical_value | decimal(15,2) | 技术价值 |
            | market_value | decimal(15,2) | 市场价值 |
            | transfer_effective_date | date | 权利转移生效日 |
            | license_type | varchar(50) | 许可类型 |
            | license_count | int | 许可次数 |
            | license_effective_date | date | 许可生效日 |
            | transferor | varchar(255) | 转让人 |
            | transferee | varchar(255) | 受让人 |
            | create_time | datetime | 创建时间 |
            | update_time | datetime | 更新时间 |
            | deleted | tinyint | 逻辑删除标志，0 表示未删除，1 表示已删除 |
            
            ### patent_info_field (字段表)
            | Column | Type | Description                                                                                                                          |
            |--------|------|--------------------------------------------------------------------------------------------------------------------------------------|
            | id | varchar(50) | 主键                                                                                                                                   |
            | patent_id | varchar(50) | 专利ID(外键)                                                                                                                             |
            | field_type | enum | 字段类型见下方“多值字段枚举” |
            | field_value | varchar(255) | 字段值                                                                                                                                  |
            | seq | int | 顺序                                                                                                                                   |
            
            多值字段枚举
            - inventor：发明人，按 `|` 拆分
            - technical_problem：[标]技术问题短语，按 `|` 拆分
            - technical_effect：[标]技术功效短语，按 `|` 拆分
            - ipc_classification：IPC分类号，按 `|` 拆分
            - cpc_classification：CPC分类号，按 `|` 拆分
            - technical_subject_classification：技术主题分类，按 `|` 拆分
            - application_field_classification：应用领域分类，按 `|` 拆分
            - ipc_main_class_interpretation：IPC主分类号(部)释义，按 `；` 或 `;` 拆分
            - strategic_industry_classification：战略新兴产业分类，按 `、` 拆分
            - technical_field：技术领域，保留为兼容字段，按 `|` 拆分


            关联: patent_info_field.patent_id = patent_info.id (一对多)
            使用: patent_info 中 deleted = 0 的为有效数据
            """;
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+(\\d+)\\b");

    private final OpenAiService openAiService;
    private final JdbcTemplate jdbcTemplate;
    private final SqlValidator sqlValidator;
    private final ReportBuilderService builderService;
    private final ObjectMapper objectMapper;

    /**
     * Generate a complete report from a user question, using AI to drive SQL generation,
     * chart selection, and analysis writing.
     */
    public AiReportResult generate(String question, List<ConversationMessage> history) {
        // Step 1: AI generates SQL queries based on question + schema
        List<SqlTask> sqlTasks = generateSqlTasks(question, history);
        log.info("AI generated {} SQL tasks", sqlTasks.size());

        // Step 2-4: Validate, execute, chart, analyze for each task
        List<AiReportResult.ReportSection> sections = new ArrayList<>();
        for (SqlTask task : sqlTasks) {
            try {
                AiReportResult.ReportSection section = executeTask(task);
                if (section != null) sections.add(section);
            } catch (Exception e) {
                log.warn("章节「{}」执行失败: {}", task.title, e.getMessage());
            }
        }

        // Step 5: Generate summary and conclusion
        String allAnalyses = sections.stream()
                .filter(s -> s.getAnalysisMarkdown() != null)
                .map(s -> "## " + s.getTitle() + "\n" + s.getAnalysisMarkdown())
                .collect(Collectors.joining("\n\n"));

        List<String> allFindings = sections.stream()
                .filter(s -> s.getKeyFindings() != null)
                .flatMap(s -> s.getKeyFindings().stream())
                .distinct()
                .collect(Collectors.toList());

        String title = generateTitle(question, sections);
        String executiveSummary = generateSummary("executive", title, question, allAnalyses);
        String conclusionSummary = generateSummary("conclusion", title, question, allAnalyses);

        return AiReportResult.builder()
                .title(title)
                .executiveSummary(executiveSummary)
                .conclusionSummary(conclusionSummary)
                .keyFindings(allFindings)
                .sections(sections)
                .build();
    }

    private List<SqlTask> generateSqlTasks(String question, List<ConversationMessage> history) {
        StringBuilder context = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            context.append("对话历史：\n");
            for (int i = Math.max(0, history.size() - 6); i < history.size(); i++) {
                ConversationMessage msg = history.get(i);
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                String c = msg.getContent();
                if (c != null && c.length() > 200) c = c.substring(0, 200) + "...";
                context.append(role).append(": ").append(c).append("\n\n");
            }
        }

        String prompt = """
                你是专利数据分析专家。根据用户的问题和数据库结构，生成一组 SQL 查询来回答用户问题。
                每个 SQL 应该独立、完整。

                数据库结构：
                %s

                用户问题：%s

                输出严格 JSON 格式（数组）：
                [
                  {
                    "title": "章节标题",
                    "objective": "该章节的分析目标(一句话)",
                    "sql": "完整的 SELECT 查询语句",
                    "chartType": "bar|line|pie|horizontal_bar|stacked_bar|scatter|treemap|radar|null"
                  }
                ]

                要求：
                1. 生成 8-20 个 SQL，根据用户问题，和数据库结构灵活选择分析维度，必
                2. 每个 SQL 必须是独立的 SELECT 查询，必须使用AS将查询的结果转换为中文
                3. 不要局限于固定的分类，根据用户问题的侧重点自由决定分析角度，但sql不能只是简单的数量分析，要凸显出复杂统计
                4. 如果用户没指定具体范围（如"全面分析"），就从不同角度
                5. chartType: bar(柱状), line(折线), pie(饼图), horizontal_bar(横向柱状),
                   stacked_bar(堆叠柱状), scatter(散点), treemap(矩形树图), radar(雷达图)
                   不适合图表的数据设置 null
                6. 使用 WHERE deleted=0 过滤有效数据
                7. 涉及 patent_info_field 时，关联条件: patent_info_field.patent_id = patent_info.id
                8. 数值字段（patent_value 等）使用 CAST(patent_value AS DECIMAL(20,2)) 转换
                9. If the user asks for Top 100 / 前100 / 100人, ranking SQL must return 100 rows; do not use LIMIT 5, 10, 15, 20, or 30.
                10. When querying inventors or IPC classifications from patent_info_field, always select pif.field_value with a clear alias, then GROUP BY/ORDER BY that alias or expression.
                11. Do not reference columns or aliases outside their query scope; every ORDER BY alias must exist in the same SELECT result.
                """.formatted(SCHEMA, question);

        if (!context.isEmpty()) {
            prompt += "\n\n" + context.toString();
        }

        try {
            String response = openAiService.chatWithJson("你只返回 JSON。", prompt);
            List<SqlTask> tasks = objectMapper.readValue(response, new TypeReference<List<SqlTask>>() {});
            return normalizeTopNTasks(question, tasks);
        } catch (Exception e) {
            log.warn("AI 生成 SQL 失败: {}", e.getMessage());
            return normalizeTopNTasks(question, generateFallbackTasks(question));
        }
    }

    private List<SqlTask> normalizeTopNTasks(String question, List<SqlTask> tasks) {
        if (!requiresTop100(question) || tasks == null || tasks.isEmpty()) return tasks;
        return tasks.stream()
                .map(task -> task.withSql(normalizeLimitTo100(task)))
                .collect(Collectors.toList());
    }

    private boolean requiresTop100(String question) {
        if (question == null) return false;
        String q = question.toLowerCase(Locale.ROOT);
        return q.contains("前100") || q.contains("top100") || q.contains("top 100") || q.contains("100人");
    }

    private String normalizeLimitTo100(SqlTask task) {
        String sql = task.sql();
        if (!StringUtils.hasText(sql)) return sql;
        Matcher matcher = LIMIT_PATTERN.matcher(sql);
        StringBuffer normalized = new StringBuffer();
        boolean changed = false;
        while (matcher.find()) {
            int limit = Integer.parseInt(matcher.group(1));
            if (limit < 100) {
                matcher.appendReplacement(normalized, "LIMIT 100");
                changed = true;
            }
        }
        matcher.appendTail(normalized);
        if (changed) return normalized.toString();

        String taskText = ((task.title() == null ? "" : task.title()) + " "
                + (task.objective() == null ? "" : task.objective())).toLowerCase(Locale.ROOT);
        boolean rankingTask = taskText.contains("排名") || taskText.contains("排行")
                || taskText.contains("top") || taskText.contains("前");
        if (!rankingTask) return sql;

        String trimmed = sql.trim();
        String suffix = trimmed.endsWith(";") ? "" : ";";
        String withoutSemicolon = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1).trim() : trimmed;
        return withoutSemicolon + " LIMIT 100" + suffix;
    }

    private List<SqlTask> generateFallbackTasks(String question) {
        List<SqlTask> tasks = new ArrayList<>();
        tasks.add(new SqlTask("专利总量概览", "统计专利总数、类型数、学院数等基本信息",
                "SELECT COUNT(*) AS total_patents, COUNT(DISTINCT college) AS college_count, " +
                "COUNT(DISTINCT patent_type) AS type_count FROM patent_info WHERE deleted=0", "bar"));
        tasks.add(new SqlTask("年度申请趋势", "分析专利申请量的年度变化",
                "SELECT application_year AS year, COUNT(*) AS count FROM patent_info " +
                "WHERE deleted=0 AND application_year IS NOT NULL AND application_year != '' " +
                "GROUP BY application_year ORDER BY application_year", "line"));
        tasks.add(new SqlTask("年度授权趋势", "分析专利授权量的年度变化",
                "SELECT grant_year AS year, COUNT(*) AS count FROM patent_info " +
                "WHERE deleted=0 AND grant_year IS NOT NULL AND grant_year != '' " +
                "GROUP BY grant_year ORDER BY grant_year", "line"));
        tasks.add(new SqlTask("专利类型分布", "分析发明专利、实用新型等类型分布",
                "SELECT patent_type AS name, COUNT(*) AS count FROM patent_info WHERE deleted=0 " +
                "AND patent_type IS NOT NULL AND patent_type != '' GROUP BY patent_type ORDER BY count DESC", "pie"));
        tasks.add(new SqlTask("法律状态分布", "分析专利的法律状态分布",
                "SELECT legal_status AS name, COUNT(*) AS count FROM patent_info WHERE deleted=0 " +
                "AND legal_status IS NOT NULL AND legal_status != '' GROUP BY legal_status ORDER BY count DESC", "pie"));
        tasks.add(new SqlTask("技术主题分布", "分析技术主题的分布情况",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count " +
                "FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id " +
                "AND pif.field_type = 'technical_subject_classification' AND pi.deleted = 0 " +
                "GROUP BY pif.field_value ORDER BY count DESC LIMIT 15", "horizontal_bar"));
        tasks.add(new SqlTask("学院产出排名", "查看各学院专利产出规模",
                "SELECT college AS name, COUNT(*) AS count FROM patent_info WHERE deleted=0 " +
                "AND college IS NOT NULL AND college != '' GROUP BY college ORDER BY count DESC LIMIT 10", "horizontal_bar"));
        tasks.add(new SqlTask("发明人活跃度排名", "查看发明人产出排名",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count " +
                "FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id " +
                "AND pif.field_type = 'inventor' AND pi.deleted = 0 " +
                "GROUP BY pif.field_value ORDER BY count DESC LIMIT 15", "horizontal_bar"));
        tasks.add(new SqlTask("高价值专利排名", "识别专利价值较高的专利",
                "SELECT pi.title AS title, pi.college AS college, " +
                "CAST(pi.patent_value AS DECIMAL(20,2)) AS patent_value FROM patent_info pi " +
                "WHERE pi.deleted=0 AND pi.patent_value IS NOT NULL AND pi.patent_value != '' " +
                "AND CAST(pi.patent_value AS DECIMAL(20,2)) > 0 " +
                "ORDER BY CAST(pi.patent_value AS DECIMAL(20,2)) DESC LIMIT 10", "horizontal_bar"));
        tasks.add(new SqlTask("战略产业分布", "分析专利在战略产业的覆盖情况",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count " +
                "FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id " +
                "AND pif.field_type = 'strategic_industry_classification' AND pi.deleted = 0 " +
                "GROUP BY pif.field_value ORDER BY count DESC LIMIT 10", "horizontal_bar"));
        return tasks;
    }

    private AiReportResult.ReportSection executeTask(SqlTask task) {
        // Validate SQL
        SqlValidator.ValidatedSql validated;
        try {
            validated = sqlValidator.validate(task.sql);
        } catch (SqlValidator.SqlValidationException e) {
            log.warn("SQL 校验失败 [{}]: {}", task.title, e.getMessage());
            return null;
        }

        // Execute SQL
        List<Map<String, Object>> data;
        try {
            data = jdbcTemplate.queryForList(validated.sql());
        } catch (Exception e) {
            log.warn("SQL 执行失败 [{}]: {}", task.title, e.getMessage());
            return null;
        }

        // Limit to 200 rows
        if (data.size() > 200) {
            data = data.subList(0, 200);
        }

        // Determine chart type (AI if null, else use given)
        String chartType = task.chartType;
        Map<String, Object> chartOption = null;
        if (chartType != null && !data.isEmpty()) {
            // Let AI pick column mapping if needed
            chartOption = builderService.buildEChartsOption(chartType, task.title, data);
        } else if (chartType == null && !data.isEmpty()) {
            // AI decides if chart is appropriate
            String decidedType = decideChartType(task.title, task.objective, data);
            if (decidedType != null) {
                chartOption = builderService.buildEChartsOption(decidedType, task.title, data);
            }
        }

        // AI writes analysis
        String analysis = generateAnalysis(task.title, task.objective, data);

        // Extract key findings
        List<String> findings = extractKeyFindings(task.title, data);

        return AiReportResult.ReportSection.builder()
                .title(task.title)
                .objective(task.objective)
                .analysisMarkdown(analysis)
                .chartOption(chartOption)
                .chartType(chartType)
                .data(data)
                .keyFindings(findings)
                .build();
    }

    private String decideChartType(String title, String objective, List<Map<String, Object>> data) {
        if (data.isEmpty()) return null;
        try {
            String dataPreview = formatDataPreview(data);
            String prompt = """
                    根据数据特征决定是否生成图表以及图表类型。
                    数据章节: %s
                    分析目标: %s
                    数据预览(前20行):
                    %s

                    输出 JSON: {"chartType": "bar|line|pie|horizontal_bar|null", "reason": "简要原因"}
                    规则:
                    - 数据少于2行 → null
                    - 分类+数值 → bar/horizontal_bar(分类名长时)
                    - 年份+数值 → line
                    - 占比类 → pie
                    - 不适合可视化 → null
                    """.formatted(title, objective, dataPreview);

            String response = openAiService.chatWithJson("你只返回 JSON。", prompt);
            Map<String, Object> result = objectMapper.readValue(response, new TypeReference<LinkedHashMap<String, Object>>() {});
            String type = (String) result.get("chartType");
            return "null".equals(type) || !StringUtils.hasText(type) ? null : type;
        } catch (Exception e) {
            log.warn("图表决策失败: {}", e.getMessage());
            return null;
        }
    }

    private String generateAnalysis(String title, String objective, List<Map<String, Object>> data) {
        try {
            String dataDesc = data.isEmpty() ? "无数据" :
                    "共 " + data.size() + " 行。\n" + formatDataPreview(data);

            String prompt = """
                    你是专利分析专家。根据以下数据和图表，写一段 100-200 字的分析。
                    章节：%s
                    目标：%s
                    数据：
                    %s

                    要求：
                    1. 聚焦数据反映的核心发现
                    2. 分析业务含义，不止复述数字
                    3. 数据不足时说明局限性
                    4. 直接输出分析内容，不要引导语
                    """.formatted(title, objective, dataDesc);

            return openAiService.chat("你是一名专业的专利分析师。", prompt);
        } catch (Exception e) {
            log.warn("AI 分析失败: {}", e.getMessage());
            return "本章节围绕「" + title + "」进行了分析，相关数据已在图表和表格中展示。";
        }
    }

    private List<String> extractKeyFindings(String title, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return List.of("暂无数据");
        List<String> findings = new ArrayList<>();
        if (data.size() == 1) return List.of(title + "：共 1 条记录。");
        try {
            List<String> keys = new ArrayList<>(data.get(0).keySet());
            if (keys.size() >= 2) {
                String valKey = keys.get(keys.size() - 1);
                String labelKey = keys.get(0);
                Map<String, Object> top = data.stream()
                        .max(Comparator.comparingDouble(r -> toNumber(r.get(valKey)).doubleValue()))
                        .orElse(data.get(0));
                findings.add(title + "中最突出的是「" + top.getOrDefault(labelKey, "-")
                        + "」，指标值 " + formatNum(top.get(valKey)) + "。");

                double total = data.stream().mapToDouble(r -> toNumber(r.get(valKey)).doubleValue()).sum();
                if (total > 0 && data.size() > 3) {
                    double top3 = data.stream()
                            .sorted((a, b) -> Double.compare(toNumber(b.get(valKey)).doubleValue(), toNumber(a.get(valKey)).doubleValue()))
                            .limit(3)
                            .mapToDouble(r -> toNumber(r.get(valKey)).doubleValue())
                            .sum();
                    findings.add("前 3 项合计占比 " + String.format(Locale.CHINA, "%.1f", top3 / total * 100) + "%。");
                }
            }
        } catch (Exception ignored) {}
        return findings;
    }

    private String generateTitle(String question, List<AiReportResult.ReportSection> sections) {
        if (StringUtils.hasText(question)) {
            String cleaned = question
                    .replaceAll("^生成|^出|^给我|^请", "")
                    .replaceAll("报告$|分析$", "")
                    .trim();
            if (cleaned.length() > 30) cleaned = cleaned.substring(0, 30);
            if (StringUtils.hasText(cleaned)) return cleaned + "分析报告";
        }
        return "专利数据分析报告";
    }

    private String generateSummary(String type, String title, String question, String allAnalyses) {
        if (!StringUtils.hasText(allAnalyses)) return "";
        try {
            String prompt = """
                    你是一个专利分析报告撰写专家。
                    报告标题：%s
                    用户需求：%s
                    章节内容：
                    %s

                    请撰写一段 %s，不超过 200 字，聚焦核心发现。
                    """.formatted(title, question, allAnalyses,
                    "executive".equals(type) ? "执行摘要" : "总结与建议");
            return openAiService.chat("你是一名专业的专利分析师。", prompt);
        } catch (Exception e) {
            log.warn("摘要生成失败: {}", e.getMessage());
            return "";
        }
    }

    private String formatDataPreview(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return "无数据";
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(20, data.size());
        for (int i = 0; i < limit; i++) {
            sb.append("行").append(i + 1).append(": ");
            sb.append(data.get(i).entrySet().stream()
                    .map(e -> e.getKey() + "=" + String.valueOf(e.getValue()).substring(0, Math.min(40, String.valueOf(e.getValue()).length())))
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
        }
        if (data.size() > limit) {
            sb.append("... 共 ").append(data.size()).append(" 行，仅展示前 ").append(limit).append(" 行用于分析预览\n");
        }
        return sb.toString();
    }

    private Number toNumber(Object value) {
        if (value instanceof Number n) return n;
        if (value == null) return 0;
        try { return Double.parseDouble(String.valueOf(value).trim().replaceAll("[,\\s]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private String formatNum(Object value) {
        if (value instanceof Number n) {
            double d = n.doubleValue();
            return Math.floor(d) == d ? String.valueOf((long) d) : String.format(Locale.CHINA, "%.2f", d);
        }
        return String.valueOf(value);
    }

    private record SqlTask(String title, String objective, String sql, String chartType) {
        private SqlTask withSql(String nextSql) {
            return new SqlTask(title, objective, nextSql, chartType);
        }
    }
}
