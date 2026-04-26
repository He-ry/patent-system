package com.example.patent.report.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.patent.report.entity.ReportRecord;
import com.example.patent.report.mapper.ReportRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportRecordService {

    private final ReportRecordMapper reportRecordMapper;

    public ReportRecord getById(String reportId) {
        return reportRecordMapper.selectById(reportId);
    }

    public List<ReportRecord> listByConversationId(String conversationId) {
        return reportRecordMapper.selectList(
                new QueryWrapper<ReportRecord>()
                        .eq("conversation_id", conversationId)
                        .orderByDesc("created_at")
        );
    }

    public void deleteById(String reportId) {
        reportRecordMapper.deleteById(reportId);
        log.info("报告记录已删除: {}", reportId);
    }
    public void bindMessage(String reportId, String messageId) {
        ReportRecord record = reportRecordMapper.selectById(reportId);
        if (record == null) {
            return;
        }
        record.setMessageId(messageId);
        reportRecordMapper.updateById(record);
    }
}
