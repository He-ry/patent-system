package com.example.patent.report.controller;

import com.example.patent.report.entity.ReportRecord;
import com.example.patent.report.service.ReportRecordService;
import com.example.patent.report.vo.ReportInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    @Value("${report.output.path:./reports}")
    private String reportDir;

    private final ReportRecordService reportRecordService;

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportInfoVO> getReport(@PathVariable String reportId) {
        try {
            ReportRecord record = reportRecordService.getById(reportId);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }

            ReportInfoVO vo = ReportInfoVO.builder()
                    .reportId(record.getId())
                    .title(record.getTitle())
                    .reportType(record.getReportType())
                    .sectionCount(record.getSectionCount())
                    .chartCount(record.getChartCount())
                    .totalWords(record.getTotalWords())
                    .createdAt(record.getCreatedAt())
                    .build();

            return ResponseEntity.ok(vo);
        } catch (Exception e) {
            log.error("获取报告信息失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<ReportInfoVO>> listByConversation(@PathVariable String conversationId) {
        try {
            List<ReportRecord> records = reportRecordService.listByConversationId(conversationId);
            List<ReportInfoVO> vos = records.stream()
                    .map(r -> ReportInfoVO.builder()
                            .reportId(r.getId())
                            .title(r.getTitle())
                            .reportType(r.getReportType())
                            .sectionCount(r.getSectionCount())
                            .chartCount(r.getChartCount())
                            .totalWords(r.getTotalWords())
                            .createdAt(r.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vos);
        } catch (Exception e) {
            log.error("获取报告列表失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{reportId}/download/html")
    public ResponseEntity<Resource> downloadHtml(@PathVariable String reportId) {
        return downloadFile(reportId, "html");
    }

    @GetMapping("/{reportId}/download/docx")
    public ResponseEntity<Resource> downloadDocx(@PathVariable String reportId) {
        return downloadFile(reportId, "docx");
    }

    @GetMapping("/{reportId}/download/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String reportId) {
        return downloadFile(reportId, "pdf");
    }

    @GetMapping("/{reportId}/view/html")
    public ResponseEntity<Resource> viewHtml(@PathVariable String reportId) {
        return serveFile(reportId, "html", true);
    }

    @GetMapping("/assets/{filename:.+}")
    public ResponseEntity<Resource> serveAsset(@PathVariable String filename) {
        try {
            File file = new File(reportDir, "charts/" + filename).getAbsoluteFile();
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            log.error("服务报告图片失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Resource> downloadFile(String reportId, String format) {
        return serveFile(reportId, format, false);
    }

    private ResponseEntity<Resource> serveFile(String reportId, String format, boolean inlineHtml) {
        try {
            ReportRecord record = reportRecordService.getById(reportId);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }

            String filepath = null;
            String contentType = null;
            String extension = null;

            switch (format.toLowerCase()) {
                case "html" -> {
                    filepath = record.getHtmlPath();
                    contentType = "text/html;charset=utf-8";
                    extension = ".html";
                }
                case "docx" -> {
                    filepath = record.getDocxPath();
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    extension = ".docx";
                }
                case "pdf" -> {
                    filepath = record.getPdfPath();
                    contentType = "application/pdf";
                    extension = ".pdf";
                }
                default -> {
                    return ResponseEntity.badRequest().build();
                }
            }

            if (filepath == null || filepath.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = new File(filepath).getAbsoluteFile();
            if (!file.exists()) {
                log.warn("报告文件不存在: {}", filepath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String filename = record.getTitle() + extension;
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(file.length())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            (inlineHtml ? "inline" : "attachment") + "; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (Exception e) {
            log.error("下载报告失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable String reportId) {
        try {
            ReportRecord record = reportRecordService.getById(reportId);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }

            deleteFileIfExists(record.getHtmlPath());
            deleteFileIfExists(record.getDocxPath());
            deleteFileIfExists(record.getPdfPath());

            reportRecordService.deleteById(reportId);
            log.info("报告已删除: {}", reportId);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("删除报告失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private void deleteFileIfExists(String filepath) {
        if (filepath != null && !filepath.isEmpty()) {
            try {
                File file = new File(filepath);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                log.warn("删除文件失败: {}", filepath, e);
            }
        }
    }
}
