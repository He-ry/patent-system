package com.example.patent.vo;

import com.example.patent.report.vo.ReportPreviewVO;
import com.example.patent.report.vo.ReportInfoVO;
import lombok.Data;
import java.util.List;

@Data
public class ChatEventVO {
    private static final String DEFAULT_SAFE_ERROR = "当前处理遇到问题，请稍后重试或调整查询条件。";
    public static final String TYPE_START = "start";
    public static final String TYPE_CONTENT = "content";
    public static final String TYPE_STATUS = "status";
    public static final String TYPE_SKILL = "skill";
    public static final String TYPE_REFERENCES = "references";
    public static final String TYPE_REPORT_PREVIEW = "report_preview";
    public static final String TYPE_REPORT = "report";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    private String type;
    private String conversationId;
    private String messageId;
    private String text;
    private String status;
    private String skillName;
    private String skillStatus;
    private Integer progress;
    private List<ReferenceGroup> references;
    private ReportPreviewVO reportPreview;
    private ReportInfoVO report;
    private String error;

    @Data
    public static class ReferenceGroup {
        private String docId;
        private String docTitle;
        private List<RefItem> items;
        private Integer count;
    }

    @Data
    public static class RefItem {
        private String id;
        private String content;
        private Double score;
    }

    public static ChatEventVO start(String convId) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_START);
        v.setConversationId(convId);
        return v;
    }

    public static ChatEventVO content(String text) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_CONTENT);
        v.setText(text);
        return v;
    }

    public static ChatEventVO status(String status) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_STATUS);
        v.setStatus(status);
        return v;
    }

    public static ChatEventVO status(String status, Integer progress) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_STATUS);
        v.setStatus(status);
        v.setProgress(progress);
        return v;
    }

    public static ChatEventVO skill(String skillName, String skillStatus) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_SKILL);
        v.setSkillName(skillName);
        v.setSkillStatus(skillStatus);
        return v;
    }

    public static ChatEventVO skill(String skillName, String skillStatus, Integer progress) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_SKILL);
        v.setSkillName(skillName);
        v.setSkillStatus(skillStatus);
        v.setProgress(progress);
        return v;
    }

    public static ChatEventVO done(String convId, String msgId) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_DONE);
        v.setConversationId(convId);
        v.setMessageId(msgId);
        return v;
    }

    public static ChatEventVO reportPreview(ReportPreviewVO preview) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_REPORT_PREVIEW);
        v.setReportPreview(preview);
        return v;
    }

    public static ChatEventVO report(String reportId, String title, String reportType, Integer sectionCount, Integer chartCount, Integer totalWords, String downloadUrl) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_REPORT);
        ReportInfoVO info = ReportInfoVO.builder()
                .reportId(reportId)
                .title(title)
                .reportType(reportType)
                .sectionCount(sectionCount)
                .chartCount(chartCount)
                .totalWords(totalWords)
                .downloadUrl(downloadUrl)
                .build();
        v.setReport(info);
        return v;
    }

    public static ChatEventVO error(String err) {
        ChatEventVO v = new ChatEventVO();
        v.setType(TYPE_ERROR);
        v.setError(err == null || err.isBlank() ? DEFAULT_SAFE_ERROR : err);
        return v;
    }

    public static ChatEventVO safeError() {
        return error(DEFAULT_SAFE_ERROR);
    }
}
