package com.example.patent.report.service;

import com.example.patent.entity.ConversationMessage;
import com.example.patent.report.domain.AiReportResult;
import com.example.patent.report.domain.ReportIntent;
import com.example.patent.report.domain.ReportRequestSpec;
import com.example.patent.report.entity.ReportRecord;
import com.example.patent.report.mapper.ReportRecordMapper;
import com.example.patent.report.vo.ReportChapterVO;
import com.example.patent.report.vo.ReportPreviewVO;
import com.example.patent.service.OpenAiService;
import com.example.patent.skill.domain.SkillExecutionResult;
import com.example.patent.skill.domain.SkillRoutingResult;
import com.example.patent.vo.ChatEventVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportOrchestrator {

    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile("(20\\d{2})\\s*[到至-]\\s*(20\\d{2})");
    private static final Pattern SINGLE_YEAR_PATTERN = Pattern.compile("(20\\d{2})年");
    private static final Pattern INVENTOR_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(?:这个人|老师|教授|博士|研究员)?(?:的发明|的专利)");
    private static final Pattern INVENTOR_ROLE_PATTERN = Pattern.compile("发明人(?:是|为)?\\s*([\\u4e00-\\u9fa5]{2,4})");
    private static final Pattern COLLEGE_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,20}(?:学院|学部|研究院))");
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(?:有关|关于|围绕|针对|聚焦|分析)\\s*([\\u4e00-\\u9fa5A-Za-z0-9]{2,20})\\s*(?:相关)?(?:专利|报告|专题)");
    private static final Set<String> INVENTOR_STOPWORDS = Set.of(
            "分析", "报告", "专利", "专利库", "主题", "有关", "相关", "人工智能", "趋势", "价值", "技术", "布局"
    );

    private final OpenAiService openAiService;
    private final JdbcTemplate jdbcTemplate;
    private final ReportBuilderService builderService;
    private final ReportRecordMapper reportRecordMapper;
    private final ObjectMapper objectMapper;
    private final IntentAnalyzer intentAnalyzer;
    private final AiReportGenerator aiReportGenerator;

    public SkillExecutionResult execute(SkillRoutingResult routing, long start,
                                        Consumer<ChatEventVO> progressCallback) {
        try {
            String rawQuery = routing != null && StringUtils.hasText(routing.getOriginalQuery())
                    ? routing.getOriginalQuery()
                    : routing != null && StringUtils.hasText(routing.getReason()) ? routing.getReason() : "";
            String conversationId = routing != null ? routing.getConversationId() : null;

            sendProgress(progressCallback, "正在分析需求并生成报告...", 5);

            List<ConversationMessage> history = fetchConversationHistory(routing);
            AiReportResult report = aiReportGenerator.generate(rawQuery, history);
            sendProgress(progressCallback, "已完成 " + report.getSections().size() + " 个章节的数据分析", 30);

            if (report.getSections().isEmpty()) {
                return SkillExecutionResult.builder()
                        .success(false).skillName("report-preview")
                        .error("未能生成有效的报告章节")
                        .executionTime(System.currentTimeMillis() - start).build();
            }

            List<ReportBuilderService.ChapterData> builderChapters = report.getSections().stream()
                    .map(s -> new ReportBuilderService.ChapterData(
                            s.getTitle(), s.getObjective(), s.getAnalysisMarkdown(),
                            s.getChartOption(), s.getData(), s.getKeyFindings()))
                    .collect(Collectors.toList());

            int totalWords = report.getSections().stream()
                    .filter(s -> s.getAnalysisMarkdown() != null)
                    .mapToInt(s -> s.getAnalysisMarkdown().length()).sum();
            int chartCount = (int) report.getSections().stream()
                    .filter(s -> s.getChartOption() != null).count();

            sendProgress(progressCallback, "正在生成 HTML 报告...", 60);
            String htmlPath = builderService.buildHtml(
                    report.getTitle(), report.getExecutiveSummary(),
                    report.getConclusionSummary(), report.getKeyFindings(), builderChapters);

            sendProgress(progressCallback, "正在生成 PDF 报告...", 75);
            String pdfPath = builderService.buildPdf(htmlPath);

            sendProgress(progressCallback, "正在生成 Word 报告...", 85);
            String docxPath = builderService.buildDocx(
                    report.getTitle(), report.getExecutiveSummary(),
                    report.getConclusionSummary(), report.getKeyFindings(), builderChapters);

            sendProgress(progressCallback, "正在保存报告记录...", 92);
            String reportId = saveReport(
                    report.getTitle(), conversationId, routing != null ? routing.getMessageId() : null,
                    htmlPath, pdfPath, docxPath,
                    report.getSections().size(), chartCount, totalWords);

            sendProgress(progressCallback, "报告生成完成", 100);

            if (progressCallback != null) {
                List<BuilderChapter> previewChapters = report.getSections().stream()
                        .map(s -> new BuilderChapter(s.getTitle(), s.getObjective(),
                                s.getAnalysisMarkdown(), s.getChartOption(), s.getData(), s.getKeyFindings()))
                        .collect(Collectors.toList());
                progressCallback.accept(ChatEventVO.reportPreview(
                        buildPreview(report.getTitle(), report.getExecutiveSummary(),
                                report.getConclusionSummary(), report.getKeyFindings(), previewChapters)));
                progressCallback.accept(ChatEventVO.report(
                        reportId, report.getTitle(), "customized",
                        report.getSections().size(), chartCount, totalWords,
                        "/api/reports/" + reportId + "/download/html"));
            }

            String summary = "报告已生成成功。\n\n"
                    + "**标题**: " + report.getTitle() + "\n"
                    + "**章节数**: " + report.getSections().size() + "\n"
                    + "**总字数**: " + totalWords + "\n\n"
                    + "当前报告已经按照用户条件生成，可在下方预览或下载 HTML、PDF、Word 三种格式。";

            return SkillExecutionResult.builder()
                    .success(true).skillName("report-preview")
                    .content(summary)
                    .executionTime(System.currentTimeMillis() - start).build();
        } catch (Exception e) {
            log.error("报告生成失败", e);
            sendProgress(progressCallback, "报告生成失败: " + e.getMessage(), 0);
            return SkillExecutionResult.builder()
                    .success(false).skillName("report-preview")
                    .error(e.getMessage())
                    .executionTime(System.currentTimeMillis() - start).build();
        }
    }

    private BuilderChapter executeChapter(ReportPlan reportPlan, ChapterPlan plan, ReportRequestSpec requestSpec, int index) {
        List<Map<String, Object>> data = queryData(plan.sql);
        String chartType = selectChartType(plan.chartType, data);

        Map<String, Object> chartOption = null;
        if (!data.isEmpty()) {
            chartOption = builderService.buildEChartsOption(chartType, plan.title, data);
        }

        String analysis = generateChapterAnalysis(reportPlan, plan, requestSpec, data, index);
        List<String> findings = extractKeyFindings(plan, data);
        return new BuilderChapter(plan.title, plan.objective, analysis, chartOption, data, findings);
    }

    private String selectChartType(String defaultType, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return defaultType;
        }
        if (data.size() <= 3) {
            return "donut";
        }
        List<String> keys = new ArrayList<>(data.get(0).keySet());
        boolean yearLike = keys.stream().anyMatch(k -> k.contains("年") || k.toLowerCase(Locale.ROOT).contains("year"));
        boolean multiSeries = keys.size() >= 3;
        boolean longLabel = !keys.isEmpty() && data.stream()
                .map(row -> String.valueOf(row.getOrDefault(keys.get(0), "")))
                .anyMatch(label -> label.length() > 8);
        if (yearLike) {
            return "line";
        }
        if (multiSeries) {
            return "stacked_bar";
        }
        if (longLabel) {
            return "horizontal_bar";
        }
        return defaultType;
    }

    private List<Map<String, Object>> queryData(String sql) {
        validateSql(sql);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.size() > 200) {
            return rows.subList(0, 200);
        }
        return rows;
    }

    private void validateSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        String upper = sql.toUpperCase(Locale.ROOT).trim();
        if (!upper.startsWith("SELECT")) {
            throw new IllegalArgumentException("只允许执行 SELECT 查询");
        }
        for (String keyword : List.of("DROP ", "DELETE ", "UPDATE ", "INSERT ", "ALTER ", "TRUNCATE ", "CREATE ")) {
            if (upper.contains(keyword)) {
                throw new IllegalArgumentException("禁止危险 SQL: " + keyword.trim());
            }
        }
    }

    private ReportRequestSpec buildRequestSpec(SkillRoutingResult routing) {
        ReportRequestSpec parsedFromExecution = parseExecutionRequest(routing == null ? null : routing.getExecution());
        String rawQuery = routing != null && StringUtils.hasText(routing.getOriginalQuery())
                ? routing.getOriginalQuery()
                : routing != null && StringUtils.hasText(routing.getReason()) ? routing.getReason() : "";

        ReportIntent intent;
        if (StringUtils.hasText(rawQuery)) {
            List<ConversationMessage> history = fetchConversationHistory(routing);
            intent = intentAnalyzer.analyze(rawQuery, history);
        } else {
            intent = new ReportIntent();
            intent.setScope("comprehensive");
            intent.setFilters(new ReportIntent.Filters());
        }

        ReportRequestSpec spec = intentToRequestSpec(intent);
        spec = mergeRequestSpecs(spec, parsedFromExecution);
        spec = validateRequestSpec(spec);

        if (!StringUtils.hasText(spec.getRawQuery())) {
            spec.setRawQuery(rawQuery);
        }
        if (!StringUtils.hasText(spec.getTitleHint())) {
            spec.setTitleHint(buildTitleHint(spec));
        }
        return spec;
    }

    private List<ConversationMessage> fetchConversationHistory(SkillRoutingResult routing) {
        if (routing == null || !StringUtils.hasText(routing.getConversationId())) return List.of();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT role, content FROM conversation_message " +
                    "WHERE conversation_id = ? AND deleted = 0 " +
                    "ORDER BY message_order ASC LIMIT 10",
                    routing.getConversationId()
            );
            List<ConversationMessage> messages = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                ConversationMessage msg = new ConversationMessage();
                msg.setRole((String) row.get("role"));
                Object content = row.get("content");
                msg.setContent(content != null ? content.toString() : "");
                messages.add(msg);
            }
            return messages;
        } catch (Exception e) {
            log.warn("获取对话历史失败: {}", e.getMessage());
            return List.of();
        }
    }

    private ReportRequestSpec intentToRequestSpec(ReportIntent intent) {
        if (intent == null) {
            return ReportRequestSpec.builder().scope("comprehensive").filters(new ReportRequestSpec.Filters()).build();
        }
        ReportRequestSpec spec = ReportRequestSpec.builder()
                .rawQuery(intent.getRawQuery())
                .titleHint(intent.getTitleHint())
                .scope(intent.getScope())
                .focus(intent.getFocus())
                .requestedDimensions(intent.getRequestedDimensions())
                .build();

        ReportIntent.Filters f = intent.getFilters();
        if (f != null) {
            spec.setFilters(ReportRequestSpec.Filters.builder()
                    .inventor(f.getInventor())
                    .college(f.getCollege())
                    .patentType(f.getPatentType())
                    .legalStatus(f.getLegalStatus())
                    .assignee(f.getAssignee())
                    .keyword(f.getKeyword())
                    .technicalField(f.getTechnicalField())
                    .ipcMainClassInterpretation(f.getIpcMainClassInterpretation())
                    .applicationYearStart(f.getApplicationYearStart())
                    .applicationYearEnd(f.getApplicationYearEnd())
                    .grantYearStart(f.getGrantYearStart())
                    .grantYearEnd(f.getGrantYearEnd())
                    .build());
        } else {
            spec.setFilters(new ReportRequestSpec.Filters());
        }
        return spec;
    }

    private ReportRequestSpec parseExecutionRequest(Map<String, Object> execution) {
        if (execution == null || execution.isEmpty()) {
            return ReportRequestSpec.builder().build();
        }
        Object reportRequest = execution.get("reportRequest");
        if (!(reportRequest instanceof Map<?, ?> requestMap)) {
            return ReportRequestSpec.builder().build();
        }
        try {
            Map<String, Object> normalized = objectMapper.convertValue(reportRequest, new TypeReference<LinkedHashMap<String, Object>>() {});
            ReportRequestSpec spec = ReportRequestSpec.builder().build();
            spec.setTitleHint(asText(normalized.get("titleHint")));
            spec.setScope(normalizeScope(asText(normalized.get("scope"))));
            spec.setFocus(toStringList(normalized.get("focus")));
            spec.setRequestedDimensions(toStringList(normalized.get("requestedDimensions")));
            spec.setFilters(parseFilters(normalized.get("filters")));
            return spec;
        } catch (IllegalArgumentException e) {
            log.warn("解析路由器报告参数失败: {}", e.getMessage());
            return ReportRequestSpec.builder().build();
        }
    }

    private ReportRequestSpec parseQueryRequest(String rawQuery) {
        ReportRequestSpec fallback = heuristicParse(rawQuery);
        if (!StringUtils.hasText(rawQuery)) {
            return validateRequestSpec(fallback);
        }

        try {
            String response = openAiService.chatWithJson(
                    "你是专利报告请求解析器，只返回 JSON。",
                    """
                            请从下面的用户原始请求中提取可用于精确定制报告的结构化条件。
                            返回 JSON：
                            {
                              "titleHint": "可选标题",
                              "scope": "inventor|college|topic|comprehensive|custom",
                              "focus": ["trend", "value", "technical_layout", "legal_status", "quality"],
                              "requestedDimensions": ["annual-trend", "patent-value-top"],
                              "filters": {
                                "inventor": "",
                                "college": "",
                                "patentType": "",
                                "legalStatus": "",
                                "assignee": "",
                                "keyword": "",
                                "technicalField": "",
                                "ipcMainClassInterpretation": "",
                                "applicationYearStart": null,
                                "applicationYearEnd": null,
                                "grantYearStart": null,
                                "grantYearEnd": null
                              }
                            }

                            要求：
                            1. 人名、学院、年份范围、法律状态、专利类型、主题关键词要尽可能提取。
                            2. 如果用户说“某个人的报告”，scope 返回 inventor。
                            3. 如果用户说“某学院的报告”，scope 返回 college。
                            4. 如果用户没有明确范围，scope 可以返回 comprehensive 或 custom。
                            5. 只返回 JSON，不要解释。

                            用户请求：
                            %s
                            """.formatted(rawQuery)
            );

            Map<String, Object> parsed = objectMapper.readValue(response, new TypeReference<LinkedHashMap<String, Object>>() {});
            ReportRequestSpec aiSpec = ReportRequestSpec.builder().build();
            aiSpec.setRawQuery(rawQuery);
            aiSpec.setTitleHint(asText(parsed.get("titleHint")));
            aiSpec.setScope(normalizeScope(asText(parsed.get("scope"))));
            aiSpec.setFocus(toStringList(parsed.get("focus")));
            aiSpec.setRequestedDimensions(toStringList(parsed.get("requestedDimensions")));
            aiSpec.setFilters(parseFilters(parsed.get("filters")));
            return validateRequestSpec(mergeRequestSpecs(fallback, aiSpec));
        } catch (Exception e) {
            log.warn("AI 解析报告请求失败，回退到规则提取: {}", e.getMessage());
            return validateRequestSpec(fallback);
        }
    }

    private ReportRequestSpec heuristicParse(String rawQuery) {
        ReportRequestSpec spec = ReportRequestSpec.builder()
                .rawQuery(Objects.toString(rawQuery, ""))
                .filters(new ReportRequestSpec.Filters())
                .build();
        if (!StringUtils.hasText(rawQuery)) {
            spec.setScope("comprehensive");
            return spec;
        }

        String query = rawQuery.trim();
        String explicitInventor = extractExplicitInventor(query);
        if (StringUtils.hasText(explicitInventor)) {
            spec.getFilters().setInventor(explicitInventor);
            spec.setScope("inventor");
        }

        Matcher collegeMatcher = COLLEGE_PATTERN.matcher(query);
        if (collegeMatcher.find()) {
            spec.getFilters().setCollege(collegeMatcher.group(1));
            spec.setScope(spec.getScope() == null ? "college" : spec.getScope());
        }

        Matcher rangeMatcher = YEAR_RANGE_PATTERN.matcher(query);
        if (rangeMatcher.find()) {
            spec.getFilters().setApplicationYearStart(Integer.parseInt(rangeMatcher.group(1)));
            spec.getFilters().setApplicationYearEnd(Integer.parseInt(rangeMatcher.group(2)));
        } else {
            Matcher singleYearMatcher = SINGLE_YEAR_PATTERN.matcher(query);
            if (singleYearMatcher.find()) {
                int year = Integer.parseInt(singleYearMatcher.group(1));
                spec.getFilters().setApplicationYearStart(year);
                spec.getFilters().setApplicationYearEnd(year);
            }
        }

        if (query.contains("已授权")) {
            spec.getFilters().setLegalStatus("已授权");
        }
        if (query.contains("发明专利")) {
            spec.getFilters().setPatentType("发明专利");
        }
        if (query.contains("实用新型")) {
            spec.getFilters().setPatentType("实用新型");
        }
        if (query.contains("趋势")) {
            spec.getFocus().add("trend");
        }
        if (query.contains("价值")) {
            spec.getFocus().add("value");
        }
        if (query.contains("技术") || query.contains("IPC")) {
            spec.getFocus().add("technical_layout");
        }

        if (!StringUtils.hasText(spec.getFilters().getInventor())
                && !StringUtils.hasText(spec.getFilters().getCollege())) {
            String inferredTopic = extractTopicKeyword(query);
            if (StringUtils.hasText(inferredTopic)) {
                spec.getFilters().setKeyword(inferredTopic);
                spec.setScope("topic");
            }
        }
        if (!StringUtils.hasText(spec.getScope())) {
            spec.setScope("comprehensive");
        }
        return spec;
    }

    private ReportRequestSpec mergeRequestSpecs(ReportRequestSpec base, ReportRequestSpec override) {
        ReportRequestSpec.Filters mergedFilters = ReportRequestSpec.Filters.builder().build();
        if (base != null && base.getFilters() != null) {
            mergedFilters = copyFilters(base.getFilters());
        }
        if (override != null && override.getFilters() != null) {
            mergedFilters = mergeFilters(mergedFilters, override.getFilters());
        }

        ReportRequestSpec merged = ReportRequestSpec.builder()
                .rawQuery(firstNonBlank(override == null ? null : override.getRawQuery(), base == null ? null : base.getRawQuery()))
                .titleHint(firstNonBlank(override == null ? null : override.getTitleHint(), base == null ? null : base.getTitleHint()))
                .scope(firstNonBlank(override == null ? null : override.getScope(), base == null ? null : base.getScope()))
                .focus(mergeStringLists(base == null ? null : base.getFocus(), override == null ? null : override.getFocus()))
                .requestedDimensions(mergeStringLists(base == null ? null : base.getRequestedDimensions(), override == null ? null : override.getRequestedDimensions()))
                .filters(mergedFilters)
                .build();

        if (!StringUtils.hasText(merged.getScope())) {
            merged.setScope("comprehensive");
        }
        return merged;
    }

    private ReportRequestSpec validateRequestSpec(ReportRequestSpec spec) {
        if (spec == null) {
            return ReportRequestSpec.builder().scope("comprehensive").filters(new ReportRequestSpec.Filters()).build();
        }
        if (spec.getFilters() == null) {
            spec.setFilters(new ReportRequestSpec.Filters());
        }

        sanitizeFilters(spec);
        normalizeScopeAndFilters(spec);
        preflightValidateFilters(spec);

        if (!StringUtils.hasText(spec.getScope())) {
            spec.setScope("comprehensive");
        }
        return spec;
    }

    private void sanitizeFilters(ReportRequestSpec spec) {
        ReportRequestSpec.Filters filters = spec.getFilters();
        String rawQuery = Objects.toString(spec.getRawQuery(), "");

        if (!looksLikeInventorName(filters.getInventor()) || !containsExplicitInventorCue(rawQuery, filters.getInventor())) {
            filters.setInventor(null);
        }
        if (StringUtils.hasText(filters.getCollege()) && !looksLikeCollege(filters.getCollege())) {
            filters.setCollege(null);
        }
        if (StringUtils.hasText(filters.getLegalStatus()) && !List.of("已授权", "审查中", "失效", "驳回", "授权").contains(filters.getLegalStatus())) {
            filters.setLegalStatus(null);
        }
        if (StringUtils.hasText(filters.getPatentType()) && !List.of("发明专利", "实用新型", "外观设计").contains(filters.getPatentType())) {
            filters.setPatentType(null);
        }
        if (StringUtils.hasText(filters.getKeyword())) {
            filters.setKeyword(cleanTopicKeyword(filters.getKeyword()));
        }
        if (StringUtils.hasText(filters.getTechnicalField())) {
            filters.setTechnicalField(cleanTopicKeyword(filters.getTechnicalField()));
        }
        if (StringUtils.hasText(filters.getIpcMainClassInterpretation())) {
            filters.setIpcMainClassInterpretation(cleanTopicKeyword(filters.getIpcMainClassInterpretation()));
        }
    }

    private void normalizeScopeAndFilters(ReportRequestSpec spec) {
        ReportRequestSpec.Filters filters = spec.getFilters();
        String scope = normalizeScope(spec.getScope());

        if ("topic".equals(scope)) {
            filters.setInventor(null);
            filters.setCollege(null);
            filters.setAssignee(null);
        }

        if ("inventor".equals(scope) && !StringUtils.hasText(filters.getInventor())) {
            spec.setScope(StringUtils.hasText(filters.getKeyword()) ? "topic" : "custom");
        } else if ("college".equals(scope) && !StringUtils.hasText(filters.getCollege())) {
            spec.setScope(StringUtils.hasText(filters.getKeyword()) ? "topic" : "custom");
        } else if (!StringUtils.hasText(scope)) {
            if (StringUtils.hasText(filters.getInventor())) {
                spec.setScope("inventor");
            } else if (StringUtils.hasText(filters.getCollege())) {
                spec.setScope("college");
            } else if (StringUtils.hasText(filters.getKeyword())
                    || StringUtils.hasText(filters.getTechnicalField())
                    || StringUtils.hasText(filters.getIpcMainClassInterpretation())) {
                spec.setScope("topic");
            } else {
                spec.setScope("comprehensive");
            }
        }
    }

    private void preflightValidateFilters(ReportRequestSpec spec) {
        ReportRequestSpec.Filters filters = spec.getFilters();
        if (filters == null) {
            return;
        }

        if (StringUtils.hasText(filters.getInventor()) && !hasInventorMatch(filters.getInventor())) {
            filters.setKeyword(firstNonBlank(filters.getKeyword(), filters.getInventor()));
            filters.setInventor(null);
            if ("inventor".equals(spec.getScope())) {
                spec.setScope("topic");
            }
        }
        if (StringUtils.hasText(filters.getCollege()) && !hasCollegeMatch(filters.getCollege())) {
            filters.setCollege(null);
            if ("college".equals(spec.getScope())) {
                spec.setScope(StringUtils.hasText(filters.getKeyword()) ? "topic" : "custom");
            }
        }
    }

    private ReportRequestSpec.Filters copyFilters(ReportRequestSpec.Filters source) {
        return ReportRequestSpec.Filters.builder()
                .inventor(source.getInventor())
                .college(source.getCollege())
                .patentType(source.getPatentType())
                .legalStatus(source.getLegalStatus())
                .assignee(source.getAssignee())
                .keyword(source.getKeyword())
                .technicalField(source.getTechnicalField())
                .ipcMainClassInterpretation(source.getIpcMainClassInterpretation())
                .applicationYearStart(source.getApplicationYearStart())
                .applicationYearEnd(source.getApplicationYearEnd())
                .grantYearStart(source.getGrantYearStart())
                .grantYearEnd(source.getGrantYearEnd())
                .build();
    }

    private ReportRequestSpec.Filters mergeFilters(ReportRequestSpec.Filters base, ReportRequestSpec.Filters override) {
        base.setInventor(firstNonBlank(override.getInventor(), base.getInventor()));
        base.setCollege(firstNonBlank(override.getCollege(), base.getCollege()));
        base.setPatentType(firstNonBlank(override.getPatentType(), base.getPatentType()));
        base.setLegalStatus(firstNonBlank(override.getLegalStatus(), base.getLegalStatus()));
        base.setAssignee(firstNonBlank(override.getAssignee(), base.getAssignee()));
        base.setKeyword(firstNonBlank(override.getKeyword(), base.getKeyword()));
        base.setTechnicalField(firstNonBlank(override.getTechnicalField(), base.getTechnicalField()));
        base.setIpcMainClassInterpretation(firstNonBlank(override.getIpcMainClassInterpretation(), base.getIpcMainClassInterpretation()));
        base.setApplicationYearStart(firstNonNull(override.getApplicationYearStart(), base.getApplicationYearStart()));
        base.setApplicationYearEnd(firstNonNull(override.getApplicationYearEnd(), base.getApplicationYearEnd()));
        base.setGrantYearStart(firstNonNull(override.getGrantYearStart(), base.getGrantYearStart()));
        base.setGrantYearEnd(firstNonNull(override.getGrantYearEnd(), base.getGrantYearEnd()));
        return base;
    }

    private ReportPlan planReport(ReportRequestSpec requestSpec) {
        List<String> selectedDimensionIds = chooseDimensions(requestSpec);
        List<ChapterPlan> chapters = new ArrayList<>();
        for (String dimensionId : selectedDimensionIds) {
            AnalysisDim dim = DIM_CATALOG.get(dimensionId);
            if (dim == null) {
                continue;
            }
            String sql = applyFilters(dim.sql, requestSpec.getFilters());
            chapters.add(new ChapterPlan(dim.title, dim.objective, sql, dim.chartType));
        }

        if (chapters.isEmpty()) {
            chapters = chooseDefaultDimensions().stream()
                    .map(DIM_CATALOG::get)
                    .filter(Objects::nonNull)
                    .map(dim -> new ChapterPlan(dim.title, dim.objective, applyFilters(dim.sql, requestSpec.getFilters()), dim.chartType))
                    .collect(Collectors.toList());
        }

        String title = buildReportTitle(requestSpec);
        return new ReportPlan(title, chapters);
    }

    private List<String> chooseDimensions(ReportRequestSpec requestSpec) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (requestSpec.getRequestedDimensions() != null) {
            requestSpec.getRequestedDimensions().stream()
                    .filter(DIM_CATALOG::containsKey)
                    .forEach(result::add);
        }

        String scope = normalizeScope(requestSpec.getScope());
        if (result.isEmpty()) {
            switch (scope) {
                case "inventor" -> result.addAll(Arrays.asList(
                        "basic-overview", "annual-trend", "patent-type-dist", "legal-status-dist",
                        "tech-field-dist", "technical-subject-dist", "application-field-dist",
                        "strategic-industry-dist", "ipc-dist", "patent-value-top", "cited-ranking", "claims-ranking"
                ));
                case "college" -> result.addAll(Arrays.asList(
                        "basic-overview", "annual-trend", "annual-grant-trend", "patent-type-dist",
                        "legal-status-dist", "tech-field-dist", "technical-subject-dist",
                        "application-field-dist", "strategic-industry-dist", "ipc-dist",
                        "inventor-ranking", "patent-value-top", "cited-ranking"
                ));
                case "topic", "custom" -> result.addAll(Arrays.asList(
                        "basic-overview", "annual-trend", "patent-type-dist", "legal-status-dist",
                        "tech-field-dist", "technical-subject-dist", "application-field-dist",
                        "strategic-industry-dist", "ipc-dist", "patent-value-top", "tech-value-top", "cited-ranking"
                ));
                default -> result.addAll(chooseDefaultDimensions());
            }
        }

        Set<String> focus = new LinkedHashSet<>(normalizeStringList(requestSpec.getFocus()));
        if (focus.contains("trend")) {
            result.add("annual-trend");
            result.add("annual-grant-trend");
        }
        if (focus.contains("value")) {
            result.add("patent-value-top");
            result.add("tech-value-top");
            result.add("market-value-top");
        }
        if (focus.contains("technical_layout")) {
            result.add("tech-field-dist");
            result.add("technical-subject-dist");
            result.add("application-field-dist");
            result.add("strategic-industry-dist");
            result.add("ipc-dist");
        }
        if (focus.contains("quality")) {
            result.add("cited-ranking");
            result.add("claims-ranking");
        }
        if (focus.contains("legal_status")) {
            result.add("legal-status-dist");
        }

        if (StringUtils.hasText(requestSpec.getFilters().getInventor())) {
            result.add("tech-field-dist");
            result.add("technical-subject-dist");
            result.add("patent-value-top");
        }
        if (StringUtils.hasText(requestSpec.getFilters().getCollege())) {
            result.add("inventor-ranking");
        }

        return result.stream()
                .filter(DIM_CATALOG::containsKey)
                .limit(12)
                .collect(Collectors.toList());
    }

    private List<String> chooseDefaultDimensions() {
        return Arrays.asList(
                "basic-overview", "annual-trend", "annual-grant-trend", "patent-type-dist",
                "legal-status-dist", "tech-field-dist", "technical-subject-dist",
                "application-field-dist", "strategic-industry-dist", "ipc-dist",
                "college-ranking", "inventor-ranking", "patent-value-top", "cited-ranking"
        );
    }

    private String applyFilters(String sql, ReportRequestSpec.Filters filters) {
        List<String> clauses = new ArrayList<>();
        if (filters == null) {
            return sql;
        }
        if (StringUtils.hasText(filters.getCollege())) {
            clauses.add("pi.college LIKE " + sqlLike(filters.getCollege()));
        }
        if (StringUtils.hasText(filters.getPatentType())) {
            clauses.add("pi.patent_type LIKE " + sqlLike(filters.getPatentType()));
        }
        if (StringUtils.hasText(filters.getLegalStatus())) {
            clauses.add("pi.legal_status LIKE " + sqlLike(filters.getLegalStatus()));
        }
        if (StringUtils.hasText(filters.getAssignee())) {
            clauses.add("(pi.current_assignee LIKE " + sqlLike(filters.getAssignee())
                    + " OR pi.original_assignee LIKE " + sqlLike(filters.getAssignee()) + ")");
        }
        if (StringUtils.hasText(filters.getKeyword())) {
            clauses.add(buildKeywordClause(filters.getKeyword()));
        }
        if (StringUtils.hasText(filters.getInventor())) {
            clauses.add(existsClause("inventor", filters.getInventor()));
        }
        if (StringUtils.hasText(filters.getTechnicalField())) {
            clauses.add(existsClause("technical_field", filters.getTechnicalField()));
        }
        if (StringUtils.hasText(filters.getIpcMainClassInterpretation())) {
            clauses.add(existsClause("ipc_main_class_interpretation", filters.getIpcMainClassInterpretation()));
        }
        if (filters.getApplicationYearStart() != null) {
            clauses.add("CAST(pi.application_year AS UNSIGNED) >= " + filters.getApplicationYearStart());
        }
        if (filters.getApplicationYearEnd() != null) {
            clauses.add("CAST(pi.application_year AS UNSIGNED) <= " + filters.getApplicationYearEnd());
        }
        if (filters.getGrantYearStart() != null) {
            clauses.add("CAST(pi.grant_year AS UNSIGNED) >= " + filters.getGrantYearStart());
        }
        if (filters.getGrantYearEnd() != null) {
            clauses.add("CAST(pi.grant_year AS UNSIGNED) <= " + filters.getGrantYearEnd());
        }

        if (clauses.isEmpty()) {
            return sql;
        }

        String filterClause = clauses.stream().collect(Collectors.joining(" AND "));
        String upper = sql.toUpperCase(Locale.ROOT);
        int insertIdx = sql.length();
        for (String keyword : List.of(" GROUP BY ", " ORDER BY ", " LIMIT ")) {
            int idx = upper.indexOf(keyword);
            if (idx >= 0 && idx < insertIdx) {
                insertIdx = idx;
            }
        }

        String before = sql.substring(0, insertIdx);
        String after = sql.substring(insertIdx);
        if (before.toUpperCase(Locale.ROOT).contains(" WHERE ")) {
            return before + " AND (" + filterClause + ")" + after;
        }
        return before + " WHERE (" + filterClause + ")" + after;
    }

    private String buildKeywordClause(String keyword) {
        List<String> clauses = new ArrayList<>();
        clauses.add("pi.title LIKE " + sqlLike(keyword));
        clauses.add(existsClause("technical_problem", keyword));
        clauses.add(existsClause("technical_effect", keyword));
        clauses.add(existsClause("technical_field", keyword));
        clauses.add(existsClause("application_field_classification", keyword));
        clauses.add(existsClause("technical_subject_classification", keyword));
        clauses.add(existsClause("ipc_main_class_interpretation", keyword));
        return "(" + String.join(" OR ", clauses) + ")";
    }

    private String existsClause(String fieldType, String value) {
        return "EXISTS (SELECT 1 FROM patent_info_field pif_filter WHERE pif_filter.patent_id = pi.id"
                + " AND pif_filter.field_type = '" + escapeSql(fieldType) + "'"
                + " AND pif_filter.field_value LIKE " + sqlLike(value) + ")";
    }

    private String generateChapterAnalysis(ReportPlan reportPlan, ChapterPlan plan, ReportRequestSpec requestSpec,
                                           List<Map<String, Object>> data, int index) {
        try {
            String prompt = """
                    你是专利分析专家。请根据以下章节信息写一段 200-400 字的专业分析。

                    报告标题：%s
                    报告对象与背景：%s
                    用户原始需求：%s
                    当前过滤条件：%s
                    章节标题：%s
                    分析目标：%s
                    本章业务含义：%s
                    数据行数：%d
                    数据摘要：
                    %s

                    要求：
                    1. 开头先点明这份分析与当前报告对象的关系，不要把对象丢掉
                    2. 结合“为什么查这个对象/主题”解释本章数据的业务含义，而不只是复述数字
                    3. 说明本章指标对该对象的专利画像、技术布局或价值判断意味着什么
                    4. 如果数据不足以支持强结论，要明确说明，不要过度推断
                    5. 尽量自然提及报告对象（如发明人、学院、主题），不要写成脱离上下文的通用统计分析
                    6. 直接输出内容，不要写“基于以上数据”等引导语
                    """.formatted(
                    reportPlan.title,
                    buildReportBackground(requestSpec),
                    defaultIfBlank(requestSpec.getRawQuery(), "未提供原始需求"),
                    describeFilters(requestSpec.getFilters()),
                    plan.title,
                    plan.objective,
                    buildChapterBusinessMeaning(plan, requestSpec),
                    data.size(),
                    formatDataSummary(data)
            );

            return openAiService.chat("你是一名专业的专利分析师。", prompt);
        } catch (Exception e) {
            log.warn("第 {} 章 AI 分析失败: {}", index, e.getMessage());
            return "本章节围绕“" + plan.title + "”进行了定制化分析，相关数据已在下方图表与表格中展示。";
        }
    }

    private List<String> extractKeyFindings(ChapterPlan plan, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return List.of("本章节暂无可展示的数据。");
        }

        List<String> findings = new ArrayList<>();
        List<String> keys = new ArrayList<>(data.get(0).keySet());

        if (data.size() == 1) {
            Map<String, Object> row = data.get(0);
            for (String key : keys) {
                Object value = row.get(key);
                if (value != null && !"0".equals(String.valueOf(value)) && !"0.0".equals(String.valueOf(value))) {
                    findings.add(translateFieldName(key) + "：" + formatDisplayValue(value));
                }
            }
            return findings;
        }

        if (keys.size() >= 2) {
            String labelKey = keys.get(0);
            String valueKey = keys.get(keys.size() - 1);

            Map<String, Object> topRow = data.stream()
                    .max((left, right) -> Double.compare(
                            toNumber(left.get(valueKey)).doubleValue(),
                            toNumber(right.get(valueKey)).doubleValue()))
                    .orElse(data.get(0));
            findings.add(plan.title + "中最突出的对象是“" + topRow.getOrDefault(labelKey, "-")
                    + "”，指标值为 " + formatDisplayValue(topRow.get(valueKey)) + "。");

            double total = data.stream()
                    .mapToDouble(row -> toNumber(row.get(valueKey)).doubleValue())
                    .sum();
            double top3 = data.stream()
                    .sorted((left, right) -> Double.compare(
                            toNumber(right.get(valueKey)).doubleValue(),
                            toNumber(left.get(valueKey)).doubleValue()))
                    .limit(3)
                    .mapToDouble(row -> toNumber(row.get(valueKey)).doubleValue())
                    .sum();
            if (total > 0) {
                findings.add("前 3 项合计占比 " + String.format(Locale.ROOT, "%.1f", top3 / total * 100) + "%。");
            }
        }

        return findings.isEmpty() ? List.of("本章节共分析 " + data.size() + " 条数据。") : findings;
    }

    private String generateSummary(String type, String title, ReportRequestSpec requestSpec, String allAnalyses) {
        String system;
        String prompt;
        if ("executive_summary".equals(type)) {
            system = "你是专利分析报告撰写专家。";
            prompt = """
                    请基于以下报告章节内容，撰写一段不超过 200 字的执行摘要。
                    标题：%s
                    报告对象与背景：%s
                    用户原始需求：%s
                    当前过滤条件：%s
                    内容：
                    %s
                    
                    要求：
                    1. 明确这份报告分析的对象是谁或什么主题，不要只写抽象结论
                    2. 先概括对象背景，再总结最重要的专利特征
                    3. 结论必须贴合当前对象，不要写成对全库的泛化判断
                    4. 数据不足时避免使用“绝对优势”“爆发式增长”等过强表述
                    """.formatted(
                    title,
                    buildReportBackground(requestSpec),
                    defaultIfBlank(requestSpec.getRawQuery(), "未提供原始需求"),
                    describeFilters(requestSpec.getFilters()),
                    allAnalyses
            );
        } else {
            system = "你是专利战略咨询专家。";
            prompt = """
                    请基于以下报告章节内容，撰写一段不超过 200 字的总结与建议。
                    标题：%s
                    报告对象与背景：%s
                    用户原始需求：%s
                    当前过滤条件：%s
                    内容：
                    %s
                    
                    要求：
                    1. 建议要围绕当前报告对象给出，避免脱离对象背景
                    2. 优先回答“这份报告对该对象意味着什么、下一步该怎么看”
                    3. 数据不足时给出审慎建议，不要强行拔高
                    """.formatted(
                    title,
                    buildReportBackground(requestSpec),
                    defaultIfBlank(requestSpec.getRawQuery(), "未提供原始需求"),
                    describeFilters(requestSpec.getFilters()),
                    allAnalyses
            );
        }
        try {
            return openAiService.chat(system, prompt);
        } catch (Exception e) {
            log.warn("AI 摘要生成失败: {}", e.getMessage());
            return "";
        }
    }

    @Transactional
    public String saveReport(String title, String conversationId, String messageId,
                             String htmlPath, String pdfPath, String docxPath,
                             int sectionCount, int chartCount, int totalWords) {
        String reportId = UUID.randomUUID().toString();
        ReportRecord record = ReportRecord.builder()
                .id(reportId)
                .conversationId(conversationId)
                .messageId(messageId)
                .title(title)
                .reportType("customized")
                .htmlPath(htmlPath)
                .pdfPath(pdfPath)
                .docxPath(docxPath)
                .sectionCount(sectionCount)
                .chartCount(chartCount)
                .totalWords(totalWords)
                .status("completed")
                .createdAt(LocalDateTime.now())
                .build();
        reportRecordMapper.insert(record);
        log.info("报告记录已保存: {}", reportId);
        return reportId;
    }

    private ReportPreviewVO buildPreview(String title, String executiveSummary, String conclusionSummary,
                                         List<String> findings, List<BuilderChapter> chapters) {
        List<ReportChapterVO> chapterVOs = chapters.stream()
                .map(chapter -> ReportChapterVO.builder()
                        .id(UUID.randomUUID().toString())
                        .title(chapter.title)
                        .objective(chapter.objective)
                        .data(chapter.data)
                        .analysisMarkdown(chapter.analysisMarkdown)
                        .chartOption(chapter.chartOption)
                        .keyFindings(chapter.keyFindings)
                        .build())
                .collect(Collectors.toList());

        return ReportPreviewVO.builder()
                .title(title)
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .executiveSummary(executiveSummary)
                .conclusionSummary(conclusionSummary)
                .keyFindings(findings)
                .chapters(chapterVOs)
                .build();
    }

    private String buildReportTitle(ReportRequestSpec spec) {
        if (StringUtils.hasText(spec.getTitleHint())) {
            return spec.getTitleHint();
        }
        if (StringUtils.hasText(spec.getFilters().getInventor())) {
            return spec.getFilters().getInventor() + " 专利发明专题报告";
        }
        if (StringUtils.hasText(spec.getFilters().getCollege())) {
            return spec.getFilters().getCollege() + " 专利分析报告";
        }
        if (StringUtils.hasText(spec.getFilters().getKeyword())) {
            return spec.getFilters().getKeyword() + " 主题专利分析报告";
        }
        return "专利定制分析报告";
    }

    private String buildTitleHint(ReportRequestSpec spec) {
        return buildReportTitle(spec);
    }

    private String extractExplicitInventor(String query) {
        Matcher roleMatcher = INVENTOR_ROLE_PATTERN.matcher(query);
        if (roleMatcher.find()) {
            String candidate = roleMatcher.group(1);
            return looksLikeInventorName(candidate) ? candidate : null;
        }

        Matcher matcher = INVENTOR_PATTERN.matcher(query);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (looksLikeInventorName(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean containsExplicitInventorCue(String rawQuery, String inventor) {
        if (!StringUtils.hasText(rawQuery) || !StringUtils.hasText(inventor)) {
            return false;
        }
        return rawQuery.contains(inventor + "的发明")
                || rawQuery.contains(inventor + "的专利")
                || rawQuery.contains(inventor + "这个人")
                || rawQuery.contains("发明人" + inventor)
                || rawQuery.contains("发明人是" + inventor)
                || rawQuery.contains("发明人为" + inventor);
    }

    private boolean looksLikeInventorName(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        String text = candidate.trim();
        if (!text.matches("[\\u4e00-\\u9fa5]{2,4}")) {
            return false;
        }
        if (INVENTOR_STOPWORDS.contains(text)) {
            return false;
        }
        for (String token : List.of("专利", "报告", "人工", "智能", "分析", "技术", "相关", "有关", "专利库")) {
            if (text.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeCollege(String college) {
        return StringUtils.hasText(college) && college.matches("[\\u4e00-\\u9fa5]{2,30}(学院|学部|研究院)");
    }

    private String extractTopicKeyword(String query) {
        Matcher topicMatcher = TOPIC_PATTERN.matcher(query);
        if (topicMatcher.find()) {
            return cleanTopicKeyword(topicMatcher.group(1));
        }
        for (String token : List.of("人工智能", "机器学习", "深度学习", "自然语言处理", "计算机视觉", "大模型")) {
            if (query.contains(token)) {
                return token;
            }
        }
        return null;
    }

    private String cleanTopicKeyword(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.trim()
                .replace("相关专利", "")
                .replace("专利报告", "")
                .replace("专题报告", "")
                .replace("当前知识库", "")
                .replace("知识库", "")
                .replace("专利", "")
                .replace("报告", "")
                .replace("专题", "")
                .replace("分析", "")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean hasInventorMatch(String inventor) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM patent_info_field WHERE field_type = 'inventor' AND field_value LIKE ?",
                    Integer.class,
                    "%" + inventor + "%"
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("Inventor preflight check failed: {}", e.getMessage());
            return true;
        }
    }

    private boolean hasCollegeMatch(String college) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM patent_info WHERE college LIKE ?",
                    Integer.class,
                    "%" + college + "%"
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("College preflight check failed: {}", e.getMessage());
            return true;
        }
    }

    private String buildReportBackground(ReportRequestSpec spec) {
        if (spec == null || spec.getFilters() == null) {
            return "这是一份围绕指定专利集合生成的定制分析报告。";
        }
        ReportRequestSpec.Filters filters = spec.getFilters();
        List<String> fragments = new ArrayList<>();
        String scope = normalizeScope(spec.getScope());
        if ("inventor".equals(scope) && StringUtils.hasText(filters.getInventor())) {
            fragments.add("本报告围绕发明人“" + filters.getInventor() + "”名下相关专利展开");
        } else if ("college".equals(scope) && StringUtils.hasText(filters.getCollege())) {
            fragments.add("本报告围绕“" + filters.getCollege() + "”的专利布局展开");
        } else if (StringUtils.hasText(filters.getKeyword())) {
            fragments.add("本报告围绕“" + filters.getKeyword() + "”相关专利主题展开");
        } else {
            fragments.add("本报告围绕当前筛选范围内的专利集合展开");
        }

        if (StringUtils.hasText(filters.getPatentType())) {
            fragments.add("重点观察“" + filters.getPatentType() + "”");
        }
        if (StringUtils.hasText(filters.getLegalStatus())) {
            fragments.add("并聚焦“" + filters.getLegalStatus() + "”状态");
        }
        if (filters.getApplicationYearStart() != null || filters.getApplicationYearEnd() != null) {
            fragments.add("申请年份范围为 " + describeYearRange(filters.getApplicationYearStart(), filters.getApplicationYearEnd()));
        }
        return String.join("，", fragments) + "。";
    }

    private String buildChapterBusinessMeaning(ChapterPlan plan, ReportRequestSpec spec) {
        String subject = resolveSubject(spec);
        return switch (plan.title) {
            case "专利总体概览" -> "用于判断" + subject + "的专利规模、覆盖范围和基础盘子。";
            case "年度申请趋势" -> "用于观察" + subject + "的研发投入节奏、持续性和阶段变化。";
            case "年度授权趋势" -> "用于判断" + subject + "的成果转化节奏和授权兑现情况。";
            case "专利类型分布" -> "用于识别" + subject + "更偏重发明专利还是其他类型，从而理解创新深度。";
            case "法律状态分布" -> "用于判断" + subject + "当前专利组合的有效性、审查进度与运营状态。";
            case "技术领域分布" -> "用于识别" + subject + "主要布局在哪些技术方向，构成怎样的技术画像。";
            case "IPC 释义分布" -> "用于补充判断" + subject + "的技术分类重心与知识产权布局结构。";
            case "高专利价值专利" -> "用于识别" + subject + "最具综合价值的代表性成果。";
            case "高技术价值专利" -> "用于识别" + subject + "技术壁垒或技术含量较高的成果。";
            case "高市场价值专利" -> "用于识别" + subject + "更可能具备转化潜力或市场空间的成果。";
            case "高被引专利排名" -> "用于判断" + subject + "哪些成果更有影响力或被后续技术参考。";
            case "权利要求数排名" -> "用于观察" + subject + "哪些成果可能具有更强的保护广度。";
            default -> "用于补充刻画" + subject + "的专利特征与布局重点。";
        };
    }

    private String describeFilters(ReportRequestSpec.Filters filters) {
        if (filters == null) {
            return "无";
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(filters.getInventor())) {
            parts.add("发明人=" + filters.getInventor());
        }
        if (StringUtils.hasText(filters.getCollege())) {
            parts.add("学院=" + filters.getCollege());
        }
        if (StringUtils.hasText(filters.getPatentType())) {
            parts.add("专利类型=" + filters.getPatentType());
        }
        if (StringUtils.hasText(filters.getLegalStatus())) {
            parts.add("法律状态=" + filters.getLegalStatus());
        }
        if (StringUtils.hasText(filters.getAssignee())) {
            parts.add("权利人/申请人=" + filters.getAssignee());
        }
        if (StringUtils.hasText(filters.getKeyword())) {
            parts.add("关键词=" + filters.getKeyword());
        }
        if (StringUtils.hasText(filters.getTechnicalField())) {
            parts.add("技术领域=" + filters.getTechnicalField());
        }
        if (StringUtils.hasText(filters.getIpcMainClassInterpretation())) {
            parts.add("IPC释义=" + filters.getIpcMainClassInterpretation());
        }
        if (filters.getApplicationYearStart() != null || filters.getApplicationYearEnd() != null) {
            parts.add("申请年份=" + describeYearRange(filters.getApplicationYearStart(), filters.getApplicationYearEnd()));
        }
        if (filters.getGrantYearStart() != null || filters.getGrantYearEnd() != null) {
            parts.add("授权年份=" + describeYearRange(filters.getGrantYearStart(), filters.getGrantYearEnd()));
        }
        return parts.isEmpty() ? "未设置额外过滤条件" : String.join("；", parts);
    }

    private String describeYearRange(Integer start, Integer end) {
        if (start != null && end != null) {
            return start.equals(end) ? String.valueOf(start) : start + "-" + end;
        }
        if (start != null) {
            return start + "及以后";
        }
        if (end != null) {
            return end + "及以前";
        }
        return "不限";
    }

    private String resolveSubject(ReportRequestSpec spec) {
        if (spec == null || spec.getFilters() == null) {
            return "当前专利集合";
        }
        if (StringUtils.hasText(spec.getFilters().getInventor())) {
            return "发明人“" + spec.getFilters().getInventor() + "”";
        }
        if (StringUtils.hasText(spec.getFilters().getCollege())) {
            return "“" + spec.getFilters().getCollege() + "”";
        }
        if (StringUtils.hasText(spec.getFilters().getKeyword())) {
            return "主题“" + spec.getFilters().getKeyword() + "”";
        }
        return "当前专利集合";
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return null;
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        if (List.of("inventor", "college", "topic", "comprehensive", "custom").contains(normalized)) {
            return normalized;
        }
        return "custom";
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::asText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        String text = asText(value);
        return StringUtils.hasText(text) ? new ArrayList<>(List.of(text)) : new ArrayList<>();
    }

    private List<String> mergeStringLists(List<String> base, List<String> override) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(normalizeStringList(base));
        result.addAll(normalizeStringList(override));
        return new ArrayList<>(result);
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private ReportRequestSpec.Filters parseFilters(Object filtersValue) {
        if (!(filtersValue instanceof Map<?, ?> filtersMap)) {
            return ReportRequestSpec.Filters.builder().build();
        }
        Map<String, Object> normalized = objectMapper.convertValue(filtersMap, new TypeReference<LinkedHashMap<String, Object>>() {});
        return ReportRequestSpec.Filters.builder()
                .inventor(asText(normalized.get("inventor")))
                .college(asText(normalized.get("college")))
                .patentType(asText(normalized.get("patentType")))
                .legalStatus(asText(normalized.get("legalStatus")))
                .assignee(asText(normalized.get("assignee")))
                .keyword(asText(normalized.get("keyword")))
                .technicalField(asText(normalized.get("technicalField")))
                .ipcMainClassInterpretation(asText(normalized.get("ipcMainClassInterpretation")))
                .applicationYearStart(asInteger(normalized.get("applicationYearStart")))
                .applicationYearEnd(asInteger(normalized.get("applicationYearEnd")))
                .grantYearStart(asInteger(normalized.get("grantYearStart")))
                .grantYearEnd(asInteger(normalized.get("grantYearEnd")))
                .build();
    }

    private String sqlLike(String value) {
        return "'%" + escapeSql(value) + "%'";
    }

    private String escapeSql(String value) {
        return Objects.toString(value, "").replace("'", "''");
    }

    private Number toNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value == null) {
            return 0;
        }
        try {
            String text = String.valueOf(value).trim().replaceAll("[,\\s]", "");
            if (text.isEmpty()) {
                return 0;
            }
            if (text.contains(".")) {
                return Double.parseDouble(text);
            }
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDataSummary(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "无数据";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, data.size());
        for (int i = 0; i < limit; i++) {
            sb.append("第 ").append(i + 1).append(" 条: ");
            sb.append(data.get(i).entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + truncate(String.valueOf(entry.getValue()), 60))
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
        }
        if (data.size() > limit) {
            sb.append("共 ").append(data.size()).append(" 条记录");
        }
        return sb.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private String translateFieldName(String key) {
        return FIELD_NAMES.getOrDefault(key, key);
    }

    private String formatDisplayValue(Object value) {
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (Math.floor(d) == d) {
                return String.valueOf((long) d);
            }
            return String.format(Locale.ROOT, "%.2f", d);
        }
        return Objects.toString(value, "-");
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private void sendProgress(Consumer<ChatEventVO> callback, String status, Integer progress) {
        if (callback != null) {
            callback.accept(ChatEventVO.status(status, progress));
        }
    }

    public record ChapterPlan(String title, String objective, String sql, String chartType) {}

    public record ReportPlan(String title, List<ChapterPlan> chapters) {}

    public record BuilderChapter(String title, String objective, String analysisMarkdown,
                                 Map<String, Object> chartOption, List<Map<String, Object>> data,
                                 List<String> keyFindings) {}

    private record AnalysisDim(String id, String title, String objective, String sql, String chartType) {}

    private static final Map<String, String> FIELD_NAMES = Map.ofEntries(
            Map.entry("college", "学院"),
            Map.entry("patent_type", "专利类型"),
            Map.entry("legal_status", "法律状态"),
            Map.entry("application_year", "申请年份"),
            Map.entry("grant_year", "授权年份"),
            Map.entry("patent_value", "专利价值"),
            Map.entry("technical_value", "技术价值"),
            Map.entry("market_value", "市场价值"),
            Map.entry("claims_count", "权利要求数"),
            Map.entry("cited_patents", "被引次数"),
            Map.entry("count", "数量"),
            Map.entry("title", "标题")
    );

    private static final Map<String, AnalysisDim> DIM_CATALOG = createCatalog();

    private static Map<String, AnalysisDim> createCatalog() {
        Map<String, AnalysisDim> map = new LinkedHashMap<>();
        map.put("basic-overview", new AnalysisDim(
                "basic-overview",
                "专利总体概览",
                "展示当前筛选范围内的专利总量、覆盖学院和专利类型数量",
                "SELECT COUNT(*) AS total_patents, COUNT(DISTINCT pi.college) AS college_count, COUNT(DISTINCT pi.patent_type) AS type_count, COUNT(DISTINCT pi.application_year) AS year_span FROM patent_info pi",
                "bar"
        ));
        map.put("annual-trend", new AnalysisDim(
                "annual-trend",
                "年度申请趋势",
                "分析筛选范围内专利申请量的年度变化趋势",
                "SELECT pi.application_year AS year, COUNT(*) AS count FROM patent_info pi WHERE pi.application_year IS NOT NULL AND pi.application_year != '' GROUP BY pi.application_year ORDER BY pi.application_year",
                "line"
        ));
        map.put("annual-grant-trend", new AnalysisDim(
                "annual-grant-trend",
                "年度授权趋势",
                "分析筛选范围内专利授权量的年度变化趋势",
                "SELECT pi.grant_year AS year, COUNT(*) AS count FROM patent_info pi WHERE pi.grant_year IS NOT NULL AND pi.grant_year != '' GROUP BY pi.grant_year ORDER BY pi.grant_year",
                "line"
        ));
        map.put("patent-type-dist", new AnalysisDim(
                "patent-type-dist",
                "专利类型分布",
                "分析发明专利、实用新型等类型分布",
                "SELECT pi.patent_type AS name, COUNT(*) AS count FROM patent_info pi WHERE pi.patent_type IS NOT NULL AND pi.patent_type != '' GROUP BY pi.patent_type ORDER BY count DESC",
                "pie"
        ));
        map.put("legal-status-dist", new AnalysisDim(
                "legal-status-dist",
                "法律状态分布",
                "分析当前筛选范围内专利的法律状态",
                "SELECT pi.legal_status AS name, COUNT(*) AS count FROM patent_info pi WHERE pi.legal_status IS NOT NULL AND pi.legal_status != '' GROUP BY pi.legal_status ORDER BY count DESC",
                "pie"
        ));
        map.put("tech-field-dist", new AnalysisDim(
                "tech-field-dist",
                "技术领域分布",
                "分析专利覆盖的技术方向",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id AND pif.field_type = 'technical_field' GROUP BY pif.field_value ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("technical-subject-dist", new AnalysisDim(
                "technical-subject-dist",
                "技术主题分类分布",
                "分析专利在技术主题分类维度的集中情况",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id AND pif.field_type = 'technical_subject_classification' GROUP BY pif.field_value ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("application-field-dist", new AnalysisDim(
                "application-field-dist",
                "应用领域分类分布",
                "分析专利在应用领域分类维度的布局情况",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id AND pif.field_type = 'application_field_classification' GROUP BY pif.field_value ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("strategic-industry-dist", new AnalysisDim(
                "strategic-industry-dist",
                "战略新兴产业分类分布",
                "分析专利在战略新兴产业分类维度的覆盖与集中情况",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id AND pif.field_type = 'strategic_industry_classification' GROUP BY pif.field_value ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("ipc-dist", new AnalysisDim(
                "ipc-dist",
                "IPC 释义分布",
                "分析 IPC 主分类释义分布情况",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id AND pif.field_type = 'ipc_main_class_interpretation' GROUP BY pif.field_value ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("college-ranking", new AnalysisDim(
                "college-ranking",
                "学院产出排名",
                "查看学院专利产出规模",
                "SELECT pi.college AS name, COUNT(*) AS count FROM patent_info pi WHERE pi.college IS NOT NULL AND pi.college != '' GROUP BY pi.college ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("inventor-ranking", new AnalysisDim(
                "inventor-ranking",
                "发明人活跃度排名",
                "查看发明人产出排名",
                "SELECT pif.field_value AS name, COUNT(DISTINCT pi.id) AS count FROM patent_info pi JOIN patent_info_field pif ON pif.patent_id = pi.id AND pif.field_type = 'inventor' GROUP BY pif.field_value ORDER BY count DESC LIMIT 20",
                "horizontal_bar"
        ));
        map.put("patent-value-top", new AnalysisDim(
                "patent-value-top",
                "高专利价值专利",
                "识别专利价值较高的专利",
                "SELECT pi.title AS title, pi.college AS college, CAST(pi.patent_value AS DECIMAL(20,2)) AS patent_value FROM patent_info pi WHERE pi.patent_value IS NOT NULL AND pi.patent_value != '' AND CAST(pi.patent_value AS DECIMAL(20,2)) > 0 ORDER BY CAST(pi.patent_value AS DECIMAL(20,2)) DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("tech-value-top", new AnalysisDim(
                "tech-value-top",
                "高技术价值专利",
                "识别技术价值较高的专利",
                "SELECT pi.title AS title, pi.college AS college, CAST(pi.technical_value AS DECIMAL(20,2)) AS technical_value FROM patent_info pi WHERE pi.technical_value IS NOT NULL AND pi.technical_value != '' AND CAST(pi.technical_value AS DECIMAL(20,2)) > 0 ORDER BY CAST(pi.technical_value AS DECIMAL(20,2)) DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("market-value-top", new AnalysisDim(
                "market-value-top",
                "高市场价值专利",
                "识别市场价值较高的专利",
                "SELECT pi.title AS title, pi.college AS college, CAST(pi.market_value AS DECIMAL(20,2)) AS market_value FROM patent_info pi WHERE pi.market_value IS NOT NULL AND pi.market_value != '' AND CAST(pi.market_value AS DECIMAL(20,2)) > 0 ORDER BY CAST(pi.market_value AS DECIMAL(20,2)) DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("cited-ranking", new AnalysisDim(
                "cited-ranking",
                "高被引专利排名",
                "查看被引用次数高的专利",
                "SELECT pi.title AS title, pi.college AS college, CAST(pi.cited_patents AS UNSIGNED) AS cited_patents FROM patent_info pi WHERE pi.cited_patents IS NOT NULL AND pi.cited_patents != '' AND CAST(pi.cited_patents AS UNSIGNED) > 0 ORDER BY CAST(pi.cited_patents AS UNSIGNED) DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("claims-ranking", new AnalysisDim(
                "claims-ranking",
                "权利要求数排名",
                "查看保护范围较大的专利",
                "SELECT pi.title AS title, pi.college AS college, CAST(pi.claims_count AS UNSIGNED) AS claims_count FROM patent_info pi WHERE pi.claims_count IS NOT NULL AND pi.claims_count != '' AND CAST(pi.claims_count AS UNSIGNED) > 0 ORDER BY CAST(pi.claims_count AS UNSIGNED) DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("province-dist", new AnalysisDim(
                "province-dist",
                "权利人省份分布",
                "分析专利地域分布",
                "SELECT pi.current_assignee_province AS name, COUNT(*) AS count FROM patent_info pi WHERE pi.current_assignee_province IS NOT NULL AND pi.current_assignee_province != '' GROUP BY pi.current_assignee_province ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("agency-ranking", new AnalysisDim(
                "agency-ranking",
                "代理机构排名",
                "分析代理机构活跃度",
                "SELECT pi.agency AS name, COUNT(*) AS count FROM patent_info pi WHERE pi.agency IS NOT NULL AND pi.agency != '' GROUP BY pi.agency ORDER BY count DESC LIMIT 15",
                "horizontal_bar"
        ));
        map.put("license-type-dist", new AnalysisDim(
                "license-type-dist",
                "许可类型分布",
                "分析许可类型分布情况",
                "SELECT pi.license_type AS name, COUNT(*) AS count FROM patent_info pi WHERE pi.license_type IS NOT NULL AND pi.license_type != '' GROUP BY pi.license_type ORDER BY count DESC",
                "pie"
        ));
        return map;
    }
}
