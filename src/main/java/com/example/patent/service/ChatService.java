package com.example.patent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.patent.config.ElasticsearchConfig;
import com.example.patent.entity.Conversation;
import com.example.patent.entity.ConversationMessage;
import com.example.patent.entity.MessageReference;
import com.example.patent.mapper.ConversationMapper;
import com.example.patent.mapper.ConversationMessageMapper;
import com.example.patent.mapper.MessageReferenceMapper;
import com.example.patent.report.service.ReportRecordService;
import com.example.patent.report.vo.ReportPreviewVO;
import com.example.patent.skill.SkillExecutor;
import com.example.patent.skill.SkillRegistry;
import com.example.patent.skill.SkillRouter;
import com.example.patent.skill.domain.SkillExecutionResult;
import com.example.patent.skill.domain.SkillRoutingResult;
import com.example.patent.vo.ChatEventVO;
import com.example.patent.vo.ConversationVO;
import com.example.patent.vo.MessageVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiService openAiService;
    private final ElasticsearchClient esClient;
    private final ElasticsearchConfig esConfig;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;
    private final MessageReferenceMapper referenceMapper;
    private final SkillRegistry skillRegistry;
    private final SkillRouter skillRouter;
    private final SkillExecutor skillExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final PatentIndexService patentIndexService;
    private final ReportRecordService reportRecordService;
    private final ObjectMapper objectMapper;

    private static final String REPORT_META_MARKER = "<!--REPORT_META:";

    @PostConstruct
    public void init() {
        log.info("ChatService initialized");
    }

    public List<ConversationVO> getConversations() {
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getDeleted, 0)
                        .orderByDesc(Conversation::getUpdatedAt)
        );
        return conversations.stream().map(this::toVO).collect(Collectors.toList());
    }

    public ConversationVO getConversation(String id) {
        Conversation conversation = conversationMapper.selectById(id);
        if (conversation == null || Objects.equals(conversation.getDeleted(), 1)) {
            return null;
        }

        ConversationVO vo = toVO(conversation);
        List<ConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, id)
                        .eq(ConversationMessage::getDeleted, 0)
                        .orderByAsc(ConversationMessage::getMessageOrder)
        );

        List<MessageVO> messageVOs = new ArrayList<>();
        for (ConversationMessage message : messages) {
            MessageVO mvo = toMessageVO(message);
            List<MessageReference> refs = referenceMapper.selectList(
                    new LambdaQueryWrapper<MessageReference>()
                            .eq(MessageReference::getMessageId, message.getId())
            );
            mvo.setReferences(refs.stream().map(this::toRefVO).collect(Collectors.toList()));
            messageVOs.add(mvo);
        }

        vo.setMessages(messageVOs);
        vo.setMessageCount((long) messageVOs.size());
        return vo;
    }

    @Transactional
    public void deleteConversation(String id) {
        Conversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            return;
        }

        List<ConversationMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, id)
        );
        for (ConversationMessage msg : messages) {
            referenceMapper.delete(
                    new LambdaQueryWrapper<MessageReference>()
                            .eq(MessageReference::getMessageId, msg.getId())
            );
        }

        messageMapper.delete(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, id)
        );
        conversationMapper.deleteById(id);
    }

    @Transactional
    public Conversation createConversation(String title) {
        Conversation conversation = new Conversation();
        conversation.setTitle(StringUtils.hasText(title) ? title : "新会话");
        conversation.setStatus("active");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    public Flux<ChatEventVO> chat(String conversationId, String content) {
        return Flux.push(sink -> runChatPipeline(conversationId, content, new FluxEventSink(sink)),
                FluxSink.OverflowStrategy.BUFFER);
    }

    public SseEmitter chatSse(String conversationId, String content) {
        SseEmitter emitter = new SseEmitter(0L);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                runChatPipeline(conversationId, content, new SseEmitterEventSink(emitter));
            } finally {
                executor.shutdown();
            }
        });
        return emitter;
    }

    private void runChatPipeline(String conversationId, String content, EventSink sink) {
        try {
            sink.emit(ChatEventVO.status("正在分析问题...", 5));

            Conversation conversation = ensureConversation(conversationId, content);
            String convId = conversation.getId();
            sink.emit(ChatEventVO.start(convId));

            int currentOrder = messageMapper.selectCount(
                    new LambdaQueryWrapper<ConversationMessage>()
                            .eq(ConversationMessage::getConversationId, convId)
                            .eq(ConversationMessage::getDeleted, 0)
            ).intValue();

            ConversationMessage userMsg = new ConversationMessage();
            userMsg.setConversationId(convId);
            userMsg.setRole("user");
            userMsg.setContent(content);
            userMsg.setMessageOrder(currentOrder + 1);
            userMsg.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(userMsg);

            sink.emit(ChatEventVO.status("正在规划执行步骤...", 18));

            String routingHistory = buildHistoryContextForRouting(convId);
            List<SkillExecutionResult> skillResults = new ArrayList<>();
            StringBuilder accumulatedSkillContext = new StringBuilder();
            final int assistantOrder = currentOrder + 2;
            final ReportPreviewVO[] reportPreviewHolder = new ReportPreviewVO[1];
            final String[] reportIdHolder = new String[1];

            for (int round = 0; round < 1; round++) {
                sink.emit(ChatEventVO.status("思考中...", 20 + round * 8));

                SkillRoutingResult routingResult = skillRouter.route(content, routingHistory);

                if (routingResult == null || !routingResult.isNeedsSkill()) {
                    break;
                }

                routingResult.setConversationId(convId);
                routingResult.setMessageId(String.valueOf(assistantOrder));
                routingResult.setOriginalQuery(content);

                String skillName = routingResult.getSkillName();
                sink.emit(ChatEventVO.skill(skillName, "正在执行", 35 + round * 12));

                SkillExecutionResult result = skillExecutor.execute(routingResult, event -> {
                    if (event != null) {
                        if (ChatEventVO.TYPE_REPORT_PREVIEW.equals(event.getType()) && event.getReportPreview() != null) {
                            reportPreviewHolder[0] = event.getReportPreview();
                        }
                        if (ChatEventVO.TYPE_REPORT.equals(event.getType()) && event.getReport() != null) {
                            reportIdHolder[0] = event.getReport().getReportId();
                        }
                        sink.emit(event);
                    }
                });
                if (result == null || !result.isSuccess()) {
                    if (result != null && StringUtils.hasText(result.getError())) {
                        sink.emit(ChatEventVO.status("执行中遇到问题，正在整理回复...", 70));
                        log.error("[Skill执行失败] skill={}, sql={}, error={}", skillName, result.getSql(), result.getError());
                        accumulatedSkillContext
                                .append("\n\n[").append(skillName).append(" 执行失败]\n")
                                .append("提示: 数据查询过程中遇到问题，请尝试调整查询条件或稍后重试。\n");
                    }
                    break;
                }

                skillResults.add(result);

                // 报告生成完成后不再执行后续轮次
                if ("report-preview".equals(skillName)) {
                    break;
                }

                if (StringUtils.hasText(result.getContent())) {
                    accumulatedSkillContext
                            .append("\n\n[")
                            .append(skillName)
                            .append("]\n")
                            .append(result.getContent());
                    routingHistory = routingHistory + "\n\n" + result.getContent();
                }
            }

            sink.emit(ChatEventVO.status("正在生成回复...", 85));

            // 如果有错误上下文，告知AI执行情况
            String errorContext = "";
            if (accumulatedSkillContext.toString().contains("执行失败")) {
                errorContext = "\n\n注意：上述技能执行过程中遇到了问题。请友好地告知用户当前无法完成该操作，建议用户检查查询条件是否正确或稍后重试。不要向用户暴露任何技术细节或错误信息。";
                sink.emit(ChatEventVO.status("技能执行遇到问题，正在整理回复...", 88));
            }
            String systemPrompt = buildSystemPrompt(
                    accumulatedSkillContext.toString() + errorContext,
                    buildHistoryContext(convId)
            );

            List<ConversationMessage> history = messageMapper.selectList(
                    new LambdaQueryWrapper<ConversationMessage>()
                            .eq(ConversationMessage::getConversationId, convId)
                            .eq(ConversationMessage::getDeleted, 0)
                            .orderByAsc(ConversationMessage::getMessageOrder)
            );

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            for (ConversationMessage msg : history) {
                messages.add(Map.of("role", msg.getRole(), "content", sanitizeStoredMessageContent(msg.getContent())));
            }
            StringBuilder answer = new StringBuilder();

            openAiService.chatStream(messages).subscribe(
                    chunk -> {
                        answer.append(chunk);
                        sink.emit(ChatEventVO.content(chunk));
                    },
                    error -> {
                        log.error("Chat stream failed", error);
                        sink.emit(ChatEventVO.error(error.getMessage()));
                        sink.complete();
                    },
                    () -> {
                        ConversationMessage assistantMsg = new ConversationMessage();
                        assistantMsg.setConversationId(convId);
                        assistantMsg.setRole("assistant");
                        assistantMsg.setContent(buildPersistedAssistantContent(answer.toString(), reportPreviewHolder[0], reportIdHolder[0]));
                        assistantMsg.setMessageOrder(assistantOrder);
                        assistantMsg.setCreatedAt(LocalDateTime.now());
                        messageMapper.insert(assistantMsg);

                        if (StringUtils.hasText(reportIdHolder[0])) {
                            reportRecordService.bindMessage(reportIdHolder[0], assistantMsg.getId());
                        }

                        persistSkillOutputs(convId, assistantMsg.getId(), skillResults);

                        conversation.setUpdatedAt(LocalDateTime.now());
                        conversationMapper.updateById(conversation);

                        sink.emit(ChatEventVO.status("处理完成", 100));
                        sink.emit(ChatEventVO.done(convId, assistantMsg.getId()));
                        sink.complete();
                    }
            );
        } catch (Exception e) {
            log.error("Chat pipeline failed", e);
            sink.emit(ChatEventVO.error(e.getMessage()));
            sink.complete();
        }
    }

    private Conversation ensureConversation(String conversationId, String content) {
        if (!StringUtils.hasText(conversationId)) {
            return createConversation(titleFromContent(content));
        }
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || Objects.equals(conversation.getDeleted(), 1)) {
            return createConversation(titleFromContent(content));
        }
        return conversation;
    }

    private String titleFromContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "新会话";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 50 ? normalized.substring(0, 50) + "..." : normalized;
    }

    private void persistSkillOutputs(String conversationId, String messageId, List<SkillExecutionResult> results) {
        for (SkillExecutionResult result : results) {
            if (result == null) {
                continue;
            }

            if (result.getData() != null && !result.getData().isEmpty()) {
                MessageReference ref = new MessageReference();
                ref.setMessageId(messageId);
                ref.setDocId(result.getSkillName());
                ref.setDocTitle("[" + result.getSkillName() + "] 执行结果");
                ref.setContent(result.getContent());
                referenceMapper.insert(ref);
            }
        }
    }

    private String buildHistoryContextForRouting(String conversationId) {
        List<ConversationMessage> history = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversationId)
                        .eq(ConversationMessage::getDeleted, 0)
                        .orderByDesc(ConversationMessage::getMessageOrder)
                        .last("LIMIT 6")
        );

        StringBuilder sb = new StringBuilder();
        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationMessage msg = history.get(i);
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            sb.append(role)
                    .append(": ")
                    .append(sanitizeStoredMessageContent(msg.getContent()))
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildHistoryContext(String conversationId) {
        List<ConversationMessage> history = messageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getConversationId, conversationId)
                        .eq(ConversationMessage::getDeleted, 0)
                        .orderByDesc(ConversationMessage::getMessageOrder)
                        .last("LIMIT 4")
        );
        if (history.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n\n最近对话上下文:\n");
        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationMessage msg = history.get(i);
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            sb.append(role)
                    .append(": ")
                    .append(sanitizeStoredMessageContent(msg.getContent()))
                    .append("\n");
        }
        return sb.toString();
    }

    public List<String> generateSuggestions(String conversationId) {
        try {
            List<ConversationMessage> history = messageMapper.selectList(
                    new LambdaQueryWrapper<ConversationMessage>()
                            .eq(ConversationMessage::getConversationId, conversationId)
                            .eq(ConversationMessage::getDeleted, 0)
                            .orderByDesc(ConversationMessage::getMessageOrder)
                            .last("LIMIT 6")
            );

            if (history.isEmpty()) {
                return List.of("查看专利总量概览", "分析技术领域分布", "查询高价值专利排名");
            }

            StringBuilder context = new StringBuilder("以下是对话历史：\n\n");
            for (int i = history.size() - 1; i >= 0; i--) {
                ConversationMessage msg = history.get(i);
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                String content = sanitizeStoredMessageContent(msg.getContent());
                // truncate long content
                if (content.length() > 300) content = content.substring(0, 300) + "...";
                context.append(role).append(": ").append(content).append("\n\n");
            }

            // Query real data statistics to ground suggestions
            Map<String, Object> stats = new LinkedHashMap<>();
            try {
                stats.put("patentCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patent_info WHERE deleted=0", Integer.class));
                stats.put("collegeCount", jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT college) FROM patent_info WHERE college IS NOT NULL AND college != ''", Integer.class));
                stats.put("inventorCount", jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT field_value) FROM patent_info_field WHERE field_type='inventor'", Integer.class));
                stats.put("topicCount", jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT field_value) FROM patent_info_field WHERE field_type='technical_subject_classification'", Integer.class));
                stats.put("yearRange", jdbcTemplate.queryForObject("SELECT CONCAT(MIN(application_year), '-', MAX(application_year)) FROM patent_info WHERE application_year IS NOT NULL AND application_year != ''", String.class));
            } catch (Exception ignored) {}

            StringBuilder dataCtx = new StringBuilder();
            dataCtx.append("\n\n当前数据库概况（用于生成贴近数据的建议）：\n");
            stats.forEach((k, v) -> dataCtx.append("- ").append(k).append(": ").append(v).append("\n"));

            context.append(dataCtx);
            context.append("基于以上对话和数据库概况，请生成 4 个用户可能想继续问的问题。");

            String prompt = """
                    你是专利数据分析助手，根据对话历史推测用户可能关心的后续问题。
                    返回严格 JSON 格式：{"suggestions": ["问题1", "问题2", "问题3", "问题4"]}

                    要求：
                    1. 生成 4 个问题
                    2. 每个问题简洁明了，20 字以内
                    3. 问题要覆盖不同维度：数据查询、分析报告、统计排名、趋势洞察等
                    4. 如果对话中已涉及具体领域，问题要围绕该领域深入
                    5. 使用中文，口语化，像用户会问的自然语言
                    6. 不要出现"如何"、"怎样"等过于开放的问题
                    """;

            String response = openAiService.chatWithJson(prompt, context.toString());
            JsonNode root = objectMapper.readTree(response);
            JsonNode suggestionsNode = root.path("suggestions");
            List<String> suggestions = new ArrayList<>();
            if (suggestionsNode.isArray()) {
                for (JsonNode node : suggestionsNode) {
                    String text = node.asText().trim();
                    if (!text.isEmpty()) {
                        suggestions.add(text);
                    }
                }
            }
            return suggestions.size() >= 2 ? suggestions : getDefaultSuggestions();
        } catch (Exception e) {
            log.warn("生成建议失败: {}", e.getMessage());
            return getDefaultSuggestions();
        }
    }

    private List<String> getDefaultSuggestions() {
        return List.of(
                "查看专利总量和技术领域分布",
                "分析近五年申请趋势",
                "查询高价值专利排名",
                "生成专利分析报告"
        );
    }

    private String buildSystemPrompt(String skillContext, String historyContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是专利智能助手，运行在专利数据分析系统上。你拥有以下能力：

                ## 能力
                1. **数据库查询**：你可以通过 SQL 直接查询 MySQL 数据库中的专利数据（patent_info 表）。
                2. **数据分析**：你可以执行统计、排名、趋势分析等数据探索操作。
                3. **报告生成**：你可以生成包含多个章节、图表和深度分析的专业专利分析报告。
                4. **知识问答**：回答专利相关的问题。

                ## 使用规则
                - 如果用户要求查看数据、统计、排名、列表等 → 系统会自动执行 SQL 查询
                - 如果用户要求"生成报告"、"出报告"、"完整分析"等 → 系统会自动生成完整报告
                - 如果用户要求"简要分析"、"分析一下"、"数据概况"等 → 系统会自动进行数据分析
                - 你只需根据执行结果整理回复即可，不需要假装无法执行

                ## 回复要求
                - 专业、准确、简洁
                - 数据用中文展示，适当使用加粗突出重点
                - 用户要求生成报告时，只需回复"报告生成中，请稍候"，不要猜测报告内容
                - 不要编造尚未执行的数据结果，只根据实际执行结果回复
                """);

        if (StringUtils.hasText(skillContext)) {
            sb.append("\n\n## 本次执行结果\n").append(skillContext);
        }
        if (StringUtils.hasText(historyContext)) {
            sb.append(historyContext);
        }
        return sb.toString();
    }


    private ConversationVO toVO(Conversation c) {
        ConversationVO vo = new ConversationVO();
        vo.setId(c.getId());
        vo.setUserId(c.getUserId());
        vo.setTitle(c.getTitle());
        vo.setStatus(c.getStatus());
        vo.setPatentIds(c.getPatentIds());
        vo.setSummary(c.getSummary());
        vo.setUploadedFiles(c.getUploadedFiles());
        vo.setCreatedAt(c.getCreatedAt());
        vo.setUpdatedAt(c.getUpdatedAt());
        return vo;
    }

    private MessageVO toMessageVO(ConversationMessage msg) {
        MessageVO vo = new MessageVO();
        vo.setId(msg.getId());
        vo.setConversationId(msg.getConversationId());
        vo.setRole(msg.getRole());
        vo.setContent(msg.getContent());
        vo.setMessageOrder(msg.getMessageOrder());
        vo.setLikes(msg.getLikes());
        vo.setDislikes(msg.getDislikes());
        vo.setBriefSummary(msg.getBriefSummary());
        vo.setCreatedAt(msg.getCreatedAt());
        return vo;
    }

    private String buildPersistedAssistantContent(String content, ReportPreviewVO reportPreview, String reportId) {
        String baseContent = StringUtils.hasText(content)
                ? content
                : (reportPreview != null ? "HTML 报告预览已生成。本预览会保留在当前会话记录中。" : "");
        if (reportPreview == null && !StringUtils.hasText(reportId)) {
            return baseContent;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (reportPreview != null) {
                payload.put("reportPreview", toPersistedReportPreview(reportPreview));
            }
            if (StringUtils.hasText(reportId)) {
                payload.put("reportId", reportId);
            }
            String json = objectMapper.writeValueAsString(payload);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return baseContent + "\n" + REPORT_META_MARKER + encoded + "-->";
        } catch (Exception e) {
            log.warn("Persisting report meta failed: {}", e.getMessage());
            return baseContent;
        }
    }

    private ReportPreviewVO toPersistedReportPreview(ReportPreviewVO reportPreview) {
        return ReportPreviewVO.builder()
                .title(reportPreview.getTitle())
                .generatedAt(reportPreview.getGeneratedAt())
                .executiveSummary(reportPreview.getExecutiveSummary())
                .conclusionSummary(reportPreview.getConclusionSummary())
                .keyFindings(reportPreview.getKeyFindings() == null ? List.of() : reportPreview.getKeyFindings().stream().limit(5).toList())
                .chapters(List.of())
                .build();
    }

    private String sanitizeStoredMessageContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        int markerIndex = content.indexOf(REPORT_META_MARKER);
        String sanitized = markerIndex >= 0 ? content.substring(0, markerIndex) : content;
        return sanitized.trim();
    }

    private MessageVO.ReferenceVO toRefVO(MessageReference ref) {
        MessageVO.ReferenceVO vo = new MessageVO.ReferenceVO();
        vo.setId(ref.getId());
        vo.setMessageId(ref.getMessageId());
        vo.setDocId(ref.getDocId());
        vo.setDocTitle(ref.getDocTitle());
        vo.setContent(ref.getContent());
        vo.setRelevanceScore(ref.getRelevanceScore());
        return vo;
    }

    private interface EventSink {
        void emit(ChatEventVO event);

        void complete();

        /** 技能执行过程中的进度回调 */
        default void reportProgress(String status, Integer progress, String skillName) {
            emit(ChatEventVO.status(status, progress));
            if (skillName != null) {
                emit(ChatEventVO.skill(skillName, status));
            }
        }

        /** 保持向后兼容，返回null表示不支持直接FluxSink操作 */
        default FluxSink<ChatEventVO> fluxSink() {
            return null;
        }
    }

    private static class SseEmitterEventSink implements EventSink {
        private final SseEmitter emitter;

        private SseEmitterEventSink(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void emit(ChatEventVO event) {
            try {
                emitter.send(SseEmitter.event().data(event));
            } catch (Exception e) {
                log.debug("SSE send skipped: {}", e.getMessage());
            }
        }

        @Override
        public void complete() {
            emitter.complete();
        }
    }

    private static class FluxEventSink implements EventSink {
        private final FluxSink<ChatEventVO> sink;

        private FluxEventSink(FluxSink<ChatEventVO> sink) {
            this.sink = sink;
        }

        @Override
        public void emit(ChatEventVO event) {
            sink.next(event);
        }

        @Override
        public void complete() {
            sink.complete();
        }

        @Override
        public FluxSink<ChatEventVO> fluxSink() {
            return sink;
        }
    }
}
