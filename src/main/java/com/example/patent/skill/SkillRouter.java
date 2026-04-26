package com.example.patent.skill;

import com.example.patent.service.OpenAiService;
import com.example.patent.skill.domain.SkillRoutingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SkillRouter {
    private static final Logger log = LoggerFactory.getLogger(SkillRouter.class);

    private final SkillRegistry registry;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    public SkillRoutingResult route(String userQuery) {
        return route(userQuery, null);
    }

    public SkillRoutingResult route(String userQuery, String historyContext) {
        if (!registry.hasSkills()) {
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("没有可用技能")
                    .build();
        }

        String normalized = userQuery == null ? "" : userQuery.toLowerCase();
        boolean explicitlyDeniesReport = normalized.contains("不要生成报告")
                || normalized.contains("不用生成报告")
                || normalized.contains("不需要报告")
                || normalized.contains("不要报告")
                || normalized.contains("别生成报告");
        boolean wantsBriefAnalysis = normalized.contains("简要分析")
                || normalized.contains("简单分析")
                || normalized.contains("分析一下")
                || normalized.contains("数据概况");

        // Simple conversation — no skill needed
        if (isSimpleConversation(normalized)) {
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("普通对话，无需技能")
                    .build();
        }

        if (explicitlyDeniesReport && !wantsBriefAnalysis) {
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("用户明确不要报告")
                    .build();
        }
        if (explicitlyDeniesReport) {
            return SkillRoutingResult.builder()
                    .needsSkill(true)
                    .skillName("data-analyzer")
                    .reason("用户需要简要分析但不要完整报告")
                    .build();
        }

        StringBuilder prompt = new StringBuilder("""
                你是专利数据智能助手的技能路由器。请根据用户需求，判断是否需要调用技能，并返回严格 JSON。

                可用技能：
                1. sql-generator
                   适用于明确的数据查询、统计、排名、列表类请求。
                2. data-analyzer
                   适用于简要分析、概况分析、数据解读，但不生成完整报告。
                3. report-preview
                   适用于用户明确要求生成报告、专题报告、完整分析报告、导出报告。
                4. none（普通对话、解释、问候、能力确认 → 此时 needsSkill 设为 false）

                ### patent_info (主表)
                | 字段名 | 类型 | 说明 |
                |---|---|---|
                | id | varchar(50) | 主键ID |
                | serial_number | varchar(50) | 序号 / 数据编号 |
                | title | varchar(255) | 专利标题 |
                | application_date | date | 申请日 |
                | inventor_count | int | 发明人数量（注意：发明人姓名不在此表，请查 patent_info_field 中 field_type='inventor'） |
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
                
                多值字段统一存储在 patent_info_field：
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

                输出 JSON 格式：
                {
                  "needsSkill": true,
                  "skillName": "sql-generator|data-analyzer|report-preview",
                  "sql": "仅当 sql-generator 或 data-analyzer 时需要",
                  "reason": "简短原因说明",
                  "execution": {
                    "reportRequest": {
                      "titleHint": "可选标题",
                      "scope": "inventor|college|topic|comprehensive|custom",
                      "focus": ["trend", "value", "technical_layout"],
                      "requestedDimensions": ["annual-trend", "tech-field-dist"],
                      "filters": {
                        "inventor": "张伟",
                        "college": "通信学院",
                        "patentType": "发明专利",
                        "legalStatus": "已授权",
                        "assignee": "某公司",
                        "keyword": "人工智能",
                        "technicalField": "智能制造",
                        "ipcMainClassInterpretation": "电学",
                        "applicationYearStart": 2022,
                        "applicationYearEnd": 2024,
                        "grantYearStart": 2023,
                        "grantYearEnd": 2024
                      }
                    }
                  }
                }

                规则：
                - 用户明确要求生成报告、专题报告、完整报告、导出报告时，使用 report-preview。
                - 用户只要简要分析，不要报告时，使用 data-analyzer。
                - 用户是查询、排名、统计、趋势、列表时，优先 sql-generator，不要走 report-preview。
                - report-preview 时，必须尽可能把用户请求中有关数据库的字段等条件提取到 execution.reportRequest.filters。
                - report-preview 时，如果用户要求“某个人/某学院/某主题的专题报告”，scope 不要返回 comprehensive。
                - 如果没有技能需要，返回 needsSkill=false。
                - 只返回 JSON，不要 markdown。
                """);

        if (historyContext != null && !historyContext.isBlank()) {
            prompt.append("\n\n历史上下文：\n").append(historyContext);
        }

        try {
            String response = openAiService.chatWithJson(prompt.toString(), userQuery);
            log.info("AI 路由响应: {}", response);
            SkillRoutingResult result = parseRoutingResult(response);
            // Hard rule: only route to report-preview if user said 报告
            if (result.isNeedsSkill() && "report-preview".equals(result.getSkillName())
                    && !userQuery.contains("报告")) {
                return SkillRoutingResult.builder()
                        .needsSkill(false)
                        .reason("用户未要求生成报告")
                        .originalQuery(userQuery)
                        .build();
            }
            result.setOriginalQuery(userQuery);
            return result;
        } catch (Exception e) {
            log.error("AI 路由失败: {}", e.getMessage());
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("路由失败: " + e.getMessage())
                    .originalQuery(userQuery)
                    .build();
        }
    }

    private SkillRoutingResult parseRoutingResult(String response) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);

            boolean needsSkill = node.path("needsSkill").asBoolean(false);
            String skillName = node.path("skillName").asText("");
            String reason = node.path("reason").asText("");
            String sql = node.path("sql").asText("");

            if (!needsSkill || skillName.isBlank() || "none".equals(skillName)) {
                return SkillRoutingResult.builder()
                        .needsSkill(false)
                        .reason(reason.isBlank() ? "不需要执行操作" : reason)
                        .build();
            }

            return SkillRoutingResult.builder()
                    .needsSkill(true)
                    .skillName(skillName)
                    .sql(sql)
                    .reason(reason)
                    .execution(parseExecution(node.path("execution")))
                    .build();
        } catch (Exception e) {
            log.error("解析路由结果失败: {}", e.getMessage());
            return SkillRoutingResult.builder()
                    .needsSkill(false)
                    .reason("解析失败: " + e.getMessage())
                    .build();
        }
    }

    private boolean isSimpleConversation(String normalized) {
        if (normalized == null || normalized.length() > 15) return false;
        return normalized.matches(".*(你好|您好|嗨|hi|hello|早上好|下午好|晚上好|谢谢|感谢|再见|拜拜|好的|ok|嗯|在吗).*");
    }

    private Map<String, Object> parseExecution(JsonNode executionNode) {
        if (executionNode == null || executionNode.isMissingNode() || executionNode.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(
                    executionNode,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
            );
        } catch (IllegalArgumentException e) {
            log.warn("解析 execution 失败: {}", e.getMessage());
            return null;
        }
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
