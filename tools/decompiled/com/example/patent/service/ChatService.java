/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ChatEventVO
 *  Conversation
 *  ConversationMapper
 *  ConversationMessage
 *  ConversationMessageMapper
 *  ConversationReportMapper
 *  ConversationVO
 *  ElasticsearchConfig
 *  MessageReference
 *  MessageReferenceMapper
 *  MessageVO
 *  MessageVO.ReferenceVO
 *  OpenAiService
 *  PatentIndexService
 *  PatentInfo
 *  PatentInfoMapper
 *  ReportPreview
 *  SkillExecutor
 *  SkillRegistry
 *  SkillRouter
 *  co.elastic.clients.elasticsearch.ElasticsearchClient
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.servlet.mvc.method.annotation.SseEmitter
 *  reactor.core.publisher.Flux
 */
package com.example.patent.service;

import MessageVO.ReferenceVO;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@Service
public class ChatService {
    @Generated
    private static final Logger log;
    private final OpenAiService openAiService;
    private final ElasticsearchClient esClient;
    private final ElasticsearchConfig esConfig;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;
    private final ConversationReportMapper reportMapper;
    private final MessageReferenceMapper referenceMapper;
    private final PatentInfoMapper patentInfoMapper;
    private final SkillRegistry skillRegistry;
    private final SkillRouter skillRouter;
    private final SkillExecutor skillExecutor;
    private final PatentIndexService patentIndexService;
    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MAX_CONTEXT_LENGTH = 2000;
    private static final int MAX_REFERENCE_LENGTH = 300;

    @PostConstruct
    public void init() {
        throw new Error("Unresolved compilation problem: \n");
    }

    private void createIndexIfNotExists() throws Exception {
        throw new Error("Unresolved compilation problem: \n\tString literal is not properly closed by a double-quote\n");
    }

    public List<ConversationVO> getConversations() {
        throw new Error("Unresolved compilation problems: \n\tConversationVO cannot be resolved to a type\n\tConversation cannot be resolved to a type\n\tConversationMapper cannot be resolved to a type\n\tConversation cannot be resolved to a type\n\tConversation cannot be resolved\n\tConversation cannot be resolved\n");
    }

    public ConversationVO getConversation(String string) {
        throw new Error("Unresolved compilation problems: \n\tConversationVO cannot be resolved to a type\n\tConversation cannot be resolved to a type\n\tConversationMapper cannot be resolved to a type\n\tConversationVO cannot be resolved to a type\n\tConversationMessage cannot be resolved to a type\n\tConversationMessageMapper cannot be resolved to a type\n\tConversationMessage cannot be resolved to a type\n\tConversationMessage cannot be resolved\n\tConversationMessage cannot be resolved\n\tConversationMessage cannot be resolved\n\tMessageVO cannot be resolved to a type\n\tConversationMessage cannot be resolved to a type\n\tMessageVO cannot be resolved to a type\n\tMessageReference cannot be resolved to a type\n\tMessageReferenceMapper cannot be resolved to a type\n\tMessageReference cannot be resolved to a type\n\tMessageReference cannot be resolved\n");
    }

    @Transactional
    public void deleteConversation(String string) {
        throw new Error("Unresolved compilation problems: \n\tConversationMapper cannot be resolved to a type\n\tConversationMessage cannot be resolved to a type\n\tConversationMessageMapper cannot be resolved to a type\n\tConversationMessage cannot be resolved to a type\n\tConversationMessage cannot be resolved\n");
    }

    @Transactional
    public Conversation createConversation(String string) {
        throw new Error("Unresolved compilation problem: \n\tConversation cannot be resolved to a type\n");
    }

    public void indexPatents(List<PatentInfo> list) {
        throw new Error("Unresolved compilation problem: \n\tPatentInfo cannot be resolved to a type\n");
    }

    String buildContent(PatentInfo patentInfo) {
        throw new Error("Unresolved compilation problem: \n\tPatentInfo cannot be resolved to a type\n");
    }

    public Flux<ChatEventVO> chat(String string, String string2) {
        throw new Error("Unresolved compilation problem: \n\tChatEventVO cannot be resolved to a type\n");
    }

    public SseEmitter chatSse(String string, String string2) {
        throw new Error("Unresolved compilation problem: \n\tString literal is not properly closed by a double-quote\n");
    }

    private String buildPatentPrompt(List<Map<String, Object>> list) {
        throw new Error("Unresolved compilation problem: \n\tString literal is not properly closed by a double-quote\n");
    }

    private String buildHistoryContextForRouting(String string) {
        throw new Error("Unresolved compilation problem: \n\tString literal is not properly closed by a double-quote\n");
    }

    private String buildHistoryContext(String string) {
        throw new Error("Unresolved compilation problem: \n\tConversationMessage cannot be resolved to a type\n");
    }

    private String buildSimpleHistoryContext(List<ConversationMessage> list) {
        throw new Error("Unresolved compilation problem: \n\tConversationMessage cannot be resolved to a type\n");
    }

    private String buildCompactHistoryContext(List<ConversationMessage> list, List<MessageReference> list2) {
        throw new Error("Unresolved compilation problem: \n\tConversationMessage cannot be resolved to a type\n");
    }

    String formatReferenceEntry(MessageReference messageReference) {
        throw new Error("Unresolved compilation problem: \n\tMessageReference cannot be resolved to a type\n");
    }

    private String generateContextSummary(List<ConversationMessage> list, List<MessageReference> list2) {
        throw new Error("Unresolved compilation problem: \n\tConversationMessage cannot be resolved to a type\n");
    }

    private String buildSystemPrompt(String string, String string2) {
        throw new Error("Unresolved compilation problem: \n\tString literal is not properly closed by a double-quote\n");
    }

    private boolean isReportIntent(String string) {
        throw new Error("Unresolved compilation problem: \n");
    }

    private boolean hasExplicitReportSpec(String string) {
        throw new Error("Unresolved compilation problem: \n\tSyntax error on tokens, delete these tokens\n");
    }

    ConversationVO toVO(Conversation conversation) {
        throw new Error("Unresolved compilation problem: \n\tConversation cannot be resolved to a type\n");
    }

    MessageVO toMessageVO(ConversationMessage conversationMessage) {
        throw new Error("Unresolved compilation problem: \n\tConversationMessage cannot be resolved to a type\n");
    }

    ReferenceVO toRefVO(MessageReference messageReference) {
        throw new Error("Unresolved compilation problem: \n\tMessageReference cannot be resolved to a type\n");
    }

    private String generateConversationSummary(String string) {
        throw new Error("Unresolved compilation problem: \n\tString literal is not properly closed by a double-quote\n");
    }

    String encodeReportPreviewToBase64(ReportPreview reportPreview) {
        throw new Error("Unresolved compilation problem: \n\tReportPreview cannot be resolved to a type\n");
    }

    @Generated
    public ChatService(OpenAiService openAiService, ElasticsearchClient elasticsearchClient, ElasticsearchConfig elasticsearchConfig, ConversationMapper conversationMapper, ConversationMessageMapper conversationMessageMapper, ConversationReportMapper conversationReportMapper, MessageReferenceMapper messageReferenceMapper, PatentInfoMapper patentInfoMapper, SkillRegistry skillRegistry, SkillRouter skillRouter, SkillExecutor skillExecutor, PatentIndexService patentIndexService) {
        throw new Error("Unresolved compilation problems: \n\tThe import com.example.patent.config cannot be resolved\n\tThe import com.example.patent.entity cannot be resolved\n\tThe import com.example.patent.entity cannot be resolved\n\tThe import com.example.patent.entity cannot be resolved\n\tThe import com.example.patent.entity cannot be resolved\n\tThe import com.example.patent.entity cannot be resolved\n\tThe import com.example.patent.mapper cannot be resolved\n\tThe import com.example.patent.mapper cannot be resolved\n\tThe import com.example.patent.mapper cannot be resolved\n\tThe import com.example.patent.mapper cannot be resolved\n\tThe import com.example.patent.mapper cannot be resolved\n\tThe import com.example.patent.skill cannot be resolved\n\tThe import com.example.patent.skill cannot be resolved\n\tThe import com.example.patent.skill cannot be resolved\n\tThe import com.example.patent.skill cannot be resolved\n\tThe import com.example.patent.reportpreview.ReportPreview cannot be resolved\n\tThe import com.example.patent.vo cannot be resolved\n\tThe import com.example.patent.vo cannot be resolved\n\tThe import com.example.patent.vo cannot be resolved\n\tOpenAiService cannot be resolved to a type\n\tConversationMessageMapper cannot be resolved to a type\n\tPatentInfoMapper cannot be resolved to a type\n\tMessageReferenceMapper cannot be resolved to a type\n\tConversationReportMapper cannot be resolved to a type\n\tConversationMapper cannot be resolved to a type\n\tSkillRegistry cannot be resolved to a type\n\tSkillRouter cannot be resolved to a type\n\tElasticsearchConfig cannot be resolved to a type\n\tSkillExecutor cannot be resolved to a type\n\tSkillRouter cannot be resolved to a type\n\tElasticsearchConfig cannot be resolved to a type\n\tConversationMapper cannot be resolved to a type\n\tSkillRegistry cannot be resolved to a type\n\tPatentInfoMapper cannot be resolved to a type\n\tOpenAiService cannot be resolved to a type\n\tSkillExecutor cannot be resolved to a type\n\tPatentIndexService cannot be resolved to a type\n\tPatentIndexService cannot be resolved to a type\n\tConversationMessageMapper cannot be resolved to a type\n\tConversationReportMapper cannot be resolved to a type\n\tMessageReferenceMapper cannot be resolved to a type\n\tOpenAiService cannot be resolved to a type\n\tElasticsearchConfig cannot be resolved to a type\n\tConversationMapper cannot be resolved to a type\n\tConversationMessageMapper cannot be resolved to a type\n\tConversationReportMapper cannot be resolved to a type\n\tMessageReferenceMapper cannot be resolved to a type\n\tPatentInfoMapper cannot be resolved to a type\n\tSkillRegistry cannot be resolved to a type\n\tSkillRouter cannot be resolved to a type\n\tSkillExecutor cannot be resolved to a type\n\tPatentIndexService cannot be resolved to a type\n");
    }
}
