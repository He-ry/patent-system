package com.example.patent.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportBuilderService {

    @Value("${echarts.server.url:http://localhost:3001}/render-save")
    private String echartsRenderUrl;

    @Value("${report.asset.base-url:/api/reports/assets/}")
    private String reportAssetBaseUrl;

    @Value("${pdf.font.path:/usr/share/fonts/simhei.ttf}")
    private String pdfFontPath;

    private static final String DOCX_FONT_FAMILY = "Microsoft YaHei";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    @Value("${report.output.path:./reports}")
    private String reportDir;

    private final ObjectMapper objectMapper;

    // ──────────────────────────────────────────────
    //  Public: build HTML, PDF, DOCX
    // ──────────────────────────────────────────────

    public String buildHtml(String reportTitle, String executiveSummary, String conclusionSummary,
                            List<String> keyFindings, List<ChapterData> chapters) {
        try {            ensureDir();
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String path = Paths.get(reportDir, "report_" + ts + ".html").toString();
            List<RenderedChapter> renderedChapters = prepareRenderedChapters(chapters);
            String html = renderHtml(reportTitle, executiveSummary, conclusionSummary, keyFindings, renderedChapters);
            Files.writeString(Paths.get(path), html);
            log.info("HTML报告已生成: {}", path);
            return path;
        } catch (Exception e) {
            throw new RuntimeException("生成HTML报告失败", e);
        }
    }

    private List<RenderedChapter> prepareRenderedChapters(List<ChapterData> chapters) {
        if (chapters == null || chapters.isEmpty()) return List.of();
        List<RenderedChapter> rendered = new ArrayList<>();
        for (ChapterData chapter : chapters) {
            rendered.add(new RenderedChapter(
                    chapter.title(),
                    chapter.objective(),
                    chapter.analysisMarkdown(),
                    chapter.chartOption(),
                    renderChartDataUrl(chapter.title(), chapter.chartOption()),
                    chapter.data(),
                    chapter.keyFindings()
            ));
        }
        return rendered;
    }

    private String renderChartDataUrl(String title, Map<String, Object> chartOption) {
        if (chartOption == null || chartOption.isEmpty()) return null;
        String filename = safeFileName(title) + "_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title);
            payload.put("width", 1200);
            payload.put("height", 680);
            payload.put("option", chartOption);
            payload.put("filename", filename);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(echartsRenderUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                Object savedFilename = result.get("filename");
                if (savedFilename != null && !String.valueOf(savedFilename).isBlank()) {
                    log.info("Chart asset generated: reports/charts/{}", savedFilename);
                    return reportAssetBaseUrl + savedFilename;
                }
            }
        } catch (Exception e) {
            log.warn("图表渲染失败: {}", e.getMessage());
        }
        return null;
    }

    private void addDocxChartImage(XWPFDocument doc, String dataUrl, String title) {
        try {
            byte[] imageBytes = loadImageBytes(dataUrl);
            if (imageBytes == null || imageBytes.length == 0) return;
            XWPFParagraph chartParagraph = doc.createParagraph();
            chartParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun chartRun = chartParagraph.createRun();
            chartRun.addPicture(new ByteArrayInputStream(imageBytes), Document.PICTURE_TYPE_PNG,
                    safeFileName(title) + ".png", Units.toEMU(460), Units.toEMU(260));
        } catch (Exception e) {
            log.warn("DOCX 图表插入失败: {}", e.getMessage());
        }
    }

    private byte[] loadImageBytes(String source) throws Exception {
        if (source == null || source.isBlank()) return null;
        if (source.startsWith("http://") || source.startsWith("https://")) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(source))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() >= 200 && response.statusCode() < 300 ? response.body() : null;
        }
        if (source.startsWith("/api/reports/assets/")) {
            String filename = source.substring("/api/reports/assets/".length());
            return Files.readAllBytes(Paths.get(reportDir, "charts", filename));
        }
        return Files.readAllBytes(Path.of(source));
    }

    private void configurePdfBuilder(PdfRendererBuilder builder) {
        builder.useFastMode();
        File fontFile = new File(pdfFontPath);
        if (fontFile.exists()) {
            builder.useFont(fontFile, "SimHei");
        }
    }

    private String safeFileName(String text) {
        if (text == null || text.isBlank()) return "chart";
        return text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    public String buildPdf(String htmlPath) {
        try {
            ensureDir();
            String pdfPath = htmlPath.replace(".html", ".pdf");
            String html = Files.readString(Paths.get(htmlPath));

            try (OutputStream os = new FileOutputStream(pdfPath)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                configurePdfBuilder(builder);
                builder.withHtmlContent(html, Paths.get(reportDir).toUri().toString());
                builder.toStream(os);
                builder.run();
            }
            log.info("PDF报告已生成: {}", pdfPath);
            return pdfPath;
        } catch (Exception e) {
            log.error("生成PDF失败: {}", e.getMessage());
            try {
                String fallbackPdfPath = htmlPath.replace(".html", ".pdf");
                String html = Files.readString(Paths.get(htmlPath));
                String simpleHtml = simplifyForPdf(html);
                try (OutputStream os = new FileOutputStream(fallbackPdfPath)) {
                    PdfRendererBuilder builder = new PdfRendererBuilder();
                    configurePdfBuilder(builder);
                    builder.withHtmlContent(simpleHtml, Paths.get(reportDir).toUri().toString());
                    builder.toStream(os);
                    builder.run();
                }
                log.info("PDF报告(简化版)已生成: {}", fallbackPdfPath);
                return fallbackPdfPath;
            } catch (Exception e2) {
                log.error("PDF生成彻底失败: {}", e2.getMessage());
                return null;
            }
        }
    }

    public String buildDocx(String reportTitle, String executiveSummary, String conclusionSummary,
                            List<String> keyFindings, List<ChapterData> chapters) {
        try {
            ensureDir();
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String path = Paths.get(reportDir, "report_" + ts + ".docx").toString();
            List<RenderedChapter> renderedChapters = prepareRenderedChapters(chapters);

            XWPFDocument doc = new XWPFDocument();

            XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            titleP.setSpacingBefore(120);
            titleP.setSpacingAfter(90);
            XWPFRun titleR = titleP.createRun();
            titleR.setText(reportTitle);
            titleR.setBold(true);
            titleR.setFontSize(26);
            titleR.setFontFamily(DOCX_FONT_FAMILY);
            titleR.setColor("0f172a");

            XWPFParagraph badgeP = doc.createParagraph();
            badgeP.setAlignment(ParagraphAlignment.CENTER);
            badgeP.setSpacingAfter(60);
            XWPFRun badgeR = badgeP.createRun();
            badgeR.setText("专利分析报告");
            badgeR.setBold(true);
            badgeR.setFontSize(11);
            badgeR.setFontFamily(DOCX_FONT_FAMILY);
            badgeR.setColor("2563eb");

            XWPFParagraph dateP = doc.createParagraph();
            dateP.setAlignment(ParagraphAlignment.CENTER);
            dateP.setSpacingAfter(260);
            XWPFRun dateR = dateP.createRun();
            dateR.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss")));
            dateR.setFontSize(10);
            dateR.setFontFamily(DOCX_FONT_FAMILY);
            dateR.setColor("94a3b8");

            if (executiveSummary != null && !executiveSummary.isBlank()) {
                addDocxSectionTitle(doc, "执行摘要", "报告摘要");
                XWPFParagraph p = doc.createParagraph();
                p.setBorderLeft(Borders.SINGLE);
                p.setIndentationLeft(220);
                p.setSpacingAfter(120);
                XWPFRun r = p.createRun();
                r.setText(executiveSummary);
                r.setFontSize(11);
                r.setFontFamily(DOCX_FONT_FAMILY);
                r.setColor("334155");
                addDocxSpacer(doc, 60);
            }

            if (keyFindings != null && !keyFindings.isEmpty()) {
                addDocxSectionTitle(doc, "核心发现", "核心发现");
                for (String f : keyFindings) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(220);
                    p.setSpacingAfter(36);
                    XWPFRun r = p.createRun();
                    r.setText("▪ " + f);
                    r.setFontSize(11);
                    r.setFontFamily(DOCX_FONT_FAMILY);
                    r.setColor("1e293b");
                }
                addDocxSpacer(doc, 60);
            }

            for (int i = 0; i < renderedChapters.size(); i++) {
                RenderedChapter ch = renderedChapters.get(i);
                addDocxSectionTitle(doc, (i + 1) + ". " + ch.title, "第" + (i + 1) + "章");
                if (ch.objective != null && !ch.objective.isBlank()) {
                    XWPFParagraph objP = doc.createParagraph();
                    objP.setSpacingAfter(72);
                    XWPFRun objR = objP.createRun();
                    objR.setText(ch.objective);
                    objR.setItalic(true);
                    objR.setColor("64748b");
                    objR.setFontSize(10);
                    objR.setFontFamily(DOCX_FONT_FAMILY);
                }
                if (ch.analysisMarkdown != null && !ch.analysisMarkdown.isBlank()) {
                    XWPFParagraph ap = doc.createParagraph();
                    ap.setSpacingAfter(100);
                    XWPFRun ar = ap.createRun();
                    String textBody = ch.analysisMarkdown.replaceAll("[*#_]", "").trim();
                    if (textBody.length() > 3000) textBody = textBody.substring(0, 3000) + "...";
                    ar.setText(textBody);
                    ar.setFontSize(11);
                    ar.setFontFamily(DOCX_FONT_FAMILY);
                    ar.setColor("334155");
                }
                if (ch.chartDataUrl != null && !ch.chartDataUrl.isBlank()) {
                    addDocxChartImage(doc, ch.chartDataUrl, ch.title);
                }
                if (ch.data != null && !ch.data.isEmpty()) {
                    buildDocxTable(doc, ch.data);
                }
                if (ch.keyFindings != null && !ch.keyFindings.isEmpty()) {
                    for (String f : ch.keyFindings) {
                        XWPFParagraph p = doc.createParagraph();
                        p.setIndentationLeft(220);
                        p.setSpacingAfter(28);
                        XWPFRun r = p.createRun();
                        r.setText("▪ " + f);
                        r.setFontSize(10);
                        r.setFontFamily(DOCX_FONT_FAMILY);
                        r.setColor("475569");
                    }
                }
                addDocxSpacer(doc, 120);
            }

            if (conclusionSummary != null && !conclusionSummary.isBlank()) {
                addDocxSectionTitle(doc, "总结与建议", "总结建议");
                XWPFParagraph p = doc.createParagraph();
                p.setBorderLeft(Borders.SINGLE);
                p.setIndentationLeft(220);
                p.setSpacingAfter(120);
                XWPFRun r = p.createRun();
                r.setText(conclusionSummary);
                r.setFontSize(11);
                r.setFontFamily(DOCX_FONT_FAMILY);
                r.setColor("334155");
            }

            try (FileOutputStream fos = new FileOutputStream(path)) {
                doc.write(fos);
            }
            doc.close();
            log.info("DOCX report generated: {}", path);
            return path;
        } catch (Exception e) {
            log.error("Failed to build DOCX report", e);
            return null;
        }
    }

    // ──────────────────────────────────────────────
    //  ECharts option generation — rich styles
    // ──────────────────────────────────────────────

    public Map<String, Object> buildEChartsOption(String chartType, String title, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return null;
        return switch (chartType) {
            case "horizontal_bar" -> buildHorizontalBar(title, data);
            case "line" -> buildLine(title, data);
            case "pie" -> buildPie(title, data);
            case "donut" -> buildDonut(title, data);
            case "scatter" -> buildScatter(title, data);
            case "treemap" -> buildTreemap(title, data);
            case "stacked_bar" -> buildStackedBar(title, data);
            case "radar" -> buildRadar(title, data);
            default -> buildBar(title, data);
        };
    }

    // — Vertical bar with gradient —
    private Map<String, Object> buildBar(String title, List<Map<String, Object>> data) {
        List<String> cats = extractCategories(data);
        List<Number> vals = extractValues(data);
        return richBase(title, "bar")
                .put("xAxis", Map.of("type", "category", "data", cats,
                        "axisLabel", Map.of("rotate", cats.size() > 6 ? 45 : 0, "interval", 0, "fontSize", 11),
                        "axisLine", Map.of("lineStyle", Map.of("color", "#e2e8f0"))))
                .put("yAxis", Map.of("type", "value",
                        "splitLine", Map.of("lineStyle", Map.of("color", "#f1f5f9"))))
                .put("series", List.of(Map.of("type", "bar", "data", vals,
                        "barMaxWidth", 44,
                        "itemStyle", Map.of(
                                "borderRadius", List.of(4, 4, 0, 0),
                                "color", Map.of("type", "linear", "x", 0, "y", 0, "x2", 0, "y2", 1,
                                        "colorStops", List.of(
                                                Map.of("offset", 0, "color", "#3b82f6"),
                                                Map.of("offset", 1, "color", "#1d4ed8")))))))
                .build();
    }

    // — Horizontal bar with dynamic label width —
    private Map<String, Object> buildHorizontalBar(String title, List<Map<String, Object>> data) {
        List<String> cats = extractCategories(data);
        List<Number> vals = extractValues(data);
        Collections.reverse(cats);
        Collections.reverse(vals);
        int maxLen = cats.stream().mapToInt(String::length).max().orElse(20);
        int leftGap = Math.min(Math.max(maxLen * 7 + 20, 120), 340);
        return richBase(title, "horizontal_bar")
                .put("yAxis", Map.of("type", "category", "data", cats, "inverse", true,
                        "axisLabel", Map.of("fontSize", 11)))
                .put("xAxis", Map.of("type", "value",
                        "splitLine", Map.of("lineStyle", Map.of("color", "#f1f5f9"))))
                .put("grid", Map.of("bottom", 30, "left", leftGap, "top", 50, "right", 30))
                .put("series", List.of(Map.of("type", "bar", "data", vals,
                        "barMaxWidth", 32,
                        "itemStyle", Map.of(
                                "borderRadius", List.of(0, 4, 4, 0),
                                "color", Map.of("type", "linear", "x", 0, "y", 0, "x2", 1, "y2", 0,
                                        "colorStops", List.of(
                                                Map.of("offset", 0, "color", "#3b82f6"),
                                                Map.of("offset", 1, "color", "#60a5fa")))))))
                .build();
    }

    // — Line with smooth curve + area fill —
    private Map<String, Object> buildLine(String title, List<Map<String, Object>> data) {
        List<String> cats = extractCategories(data);
        List<Number> vals = extractValues(data);
        return richBase(title, "line")
                .put("xAxis", Map.of("type", "category", "data", cats,
                        "axisLabel", Map.of("rotate", cats.size() > 8 ? 45 : 0, "interval", 0),
                        "axisLine", Map.of("lineStyle", Map.of("color", "#e2e8f0"))))
                .put("yAxis", Map.of("type", "value",
                        "splitLine", Map.of("lineStyle", Map.of("color", "#f1f5f9"))))
                .put("dataZoom", List.of(Map.of("type", "inside"), Map.of("type", "slider", "height", 20, "bottom", 0)))
                .put("series", List.of(Map.of("type", "line", "data", vals,
                        "smooth", true, "symbol", "circle", "symbolSize", 6,
                        "lineStyle", Map.of("width", 3, "color", "#3b82f6"),
                        "areaStyle", Map.of("color", Map.of("type", "linear", "x", 0, "y", 0, "x2", 0, "y2", 1,
                                "colorStops", List.of(
                                        Map.of("offset", 0, "color", "rgba(59,130,246,0.3)"),
                                        Map.of("offset", 1, "color", "rgba(59,130,246,0.02)")))),
                        "itemStyle", Map.of("color", "#3b82f6"))))
                .build();
    }

    // — Pie —
    private Map<String, Object> buildPie(String title, List<Map<String, Object>> data) {
        List<Map<String, Object>> pieData = buildPieData(data);
        if (pieData.isEmpty()) return null;
        return richBase(title, "pie")
                .put("tooltip", Map.of("trigger", "item", "formatter", "{b}: {c}件 ({d}%)"))
                .put("legend", Map.of("type", "scroll", "top", "bottom", "textStyle", Map.of("fontSize", 11)))
                .put("series", List.of(Map.of("type", "pie",
                        "radius", List.of("0%", "65%"),
                        "center", List.of("50%", "46%"),
                        "label", Map.of("formatter", "{b}: {d}%", "fontSize", 12),
                        "labelLine", Map.of("length", 15, "length2", 10),
                        "data", pieData,
                        "emphasis", Map.of("itemStyle", Map.of("shadowBlur", 10, "shadowColor", "rgba(0,0,0,0.15)")),
                        "animationType", "scale")))
                .build();
    }

    // — Donut (nicer pie) —
    private Map<String, Object> buildDonut(String title, List<Map<String, Object>> data) {
        List<Map<String, Object>> pieData = buildPieData(data);
        if (pieData.isEmpty()) return null;
        double total = pieData.stream()
                .mapToDouble(m -> ((Number) m.get("value")).doubleValue())
                .sum();
        return richBase(title, "donut")
                .put("tooltip", Map.of("trigger", "item", "formatter", "{b}: {c}件 ({d}%)"))
                .put("legend", Map.of("type", "scroll", "top", "bottom", "textStyle", Map.of("fontSize", 11)))
                .put("graphic", List.of(Map.of("type", "text", "left", "center", "top", "44%",
                        "style", Map.of("text", String.format("%.0f", total), "textAlign", "center",
                                "fontSize", 22, "fontWeight", "bold", "fill", "#1e293b"))))
                .put("series", List.of(Map.of("type", "pie",
                        "radius", List.of("40%", "68%"),
                        "center", List.of("50%", "48%"),
                        "label", Map.of("formatter", "{b}: {d}%", "fontSize", 11),
                        "labelLine", Map.of("length", 12, "length2", 8),
                        "data", pieData,
                        "emphasis", Map.of("itemStyle", Map.of("shadowBlur", 10, "shadowColor", "rgba(0,0,0,0.15)")),
                        "animationType", "scale")))
                .build();
    }

    // — Scatter —
    private Map<String, Object> buildScatter(String title, List<Map<String, Object>> data) {
        List<String> keys = new ArrayList<>(data.get(0).keySet());
        List<List<Object>> scatterData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Number x = keys.size() > 0 ? toNumber(row.get(keys.get(0))) : 0;
            Number y = keys.size() > 1 ? toNumber(row.get(keys.get(1))) : 0;
            String name = keys.size() > 2 ? String.valueOf(row.get(keys.get(2))) : "";
            scatterData.add(List.of(x, y, name));
        }
        return richBase(title, "scatter")
                .put("xAxis", Map.of("type", "value", "name", translateField(keys.get(0)),
                        "splitLine", Map.of("lineStyle", Map.of("color", "#f1f5f9"))))
                .put("yAxis", Map.of("type", "value", "name", keys.size() > 1 ? translateField(keys.get(1)) : "",
                        "splitLine", Map.of("lineStyle", Map.of("color", "#f1f5f9"))))
                .put("series", List.of(Map.of("type", "scatter", "symbolSize", 10,
                        "data", scatterData,
                        "itemStyle", Map.of("color", Map.of("type", "linear", "x", 0, "y", 0, "x2", 0, "y2", 1,
                                "colorStops", List.of(
                                        Map.of("offset", 0, "color", "#3b82f6"),
                                        Map.of("offset", 1, "color", "#8b5cf6")))))))
                .build();
    }

    // — Treemap —
    private Map<String, Object> buildTreemap(String title, List<Map<String, Object>> data) {
        List<Map<String, Object>> treeData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            List<String> keys = new ArrayList<>(row.keySet());
            String name = keys.size() > 0 ? String.valueOf(row.get(keys.get(0))) : "";
            Number v = keys.size() > 1 ? toNumber(row.get(keys.get(1))) : 0;
            if (!name.isBlank() && v.doubleValue() > 0) treeData.add(Map.of("name", name, "value", v));
        }
        return richBase(title, "treemap")
                .put("series", List.of(Map.of("type", "treemap", "data", treeData,
                        "roam", true, "breadcrumb", Map.of("show", false),
                        "label", Map.of("show", true, "fontSize", 11, "color", "#fff"),
                        "itemStyle", Map.of("borderColor", "#fff", "borderWidth", 2))))
                .build();
    }

    // — Stacked bar —
    private Map<String, Object> buildStackedBar(String title, List<Map<String, Object>> data) {
        // Expects: category, series_name, value columns
        // Group by category and stack series
        List<String> keys = new ArrayList<>(data.get(0).keySet());
        if (keys.size() < 3) return buildBar(title, data);
        String catKey = keys.get(0);
        String seriesKey = keys.get(1);
        String valKey = keys.get(2);

        // Collect unique categories and series
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        LinkedHashSet<String> seriesNames = new LinkedHashSet<>();
        Map<String, Map<String, Number>> matrix = new LinkedHashMap<>();
        for (Map<String, Object> row : data) {
            String cat = String.valueOf(row.getOrDefault(catKey, ""));
            String s = String.valueOf(row.getOrDefault(seriesKey, ""));
            Number v = toNumber(row.get(valKey));
            categories.add(cat);
            seriesNames.add(s);
            matrix.computeIfAbsent(cat, k -> new LinkedHashMap<>()).put(s, v);
        }

        List<String> catList = new ArrayList<>(categories);
        List<Map<String, Object>> series = new ArrayList<>();
        int colorIdx = 0;
        for (String s : seriesNames) {
            List<Number> sData = new ArrayList<>();
            for (String c : catList) {
                sData.add(matrix.getOrDefault(c, Map.of()).getOrDefault(s, 0));
            }
            series.add(Map.of("type", "bar", "name", s, "data", sData,
                    "stack", "total", "barMaxWidth", 36,
                    "itemStyle", Map.of("borderRadius", List.of(0, 0, 0, 0)),
                    "color", getColor(colorIdx++)));
        }

        return richBase(title, "bar")
                .put("tooltip", Map.of("trigger", "axis", "axisPointer", Map.of("type", "shadow")))
                .put("legend", Map.of("right", 0, "top", 28))
                .put("xAxis", Map.of("type", "category", "data", catList,
                        "axisLabel", Map.of("rotate", 35, "interval", 0)))
                .put("yAxis", Map.of("type", "value", "splitLine", Map.of("lineStyle", Map.of("color", "#f1f5f9"))))
                .put("series", series)
                .build();
    }

    // — Radar —
    private Map<String, Object> buildRadar(String title, List<Map<String, Object>> data) {
        List<String> keys = new ArrayList<>(data.get(0).keySet());
        if (keys.size() < 2) return buildBar(title, data);
        // First key → indicator names, rest → values
        String indicatorKey = keys.get(0);
        List<Map<String, Object>> indicators = new ArrayList<>();
        for (String k : keys.subList(1, keys.size())) {
            indicators.add(Map.of("name", translateField(k), "max", 100));
        }
        List<Map<String, Object>> seriesData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            String name = String.valueOf(row.getOrDefault(indicatorKey, ""));
            List<Number> vals = new ArrayList<>();
            for (int i = 1; i < keys.size(); i++) {
                vals.add(toNumber(row.get(keys.get(i))));
            }
            seriesData.add(Map.of("value", vals, "name", name));
        }
        return richBase(title, "radar")
                .put("radar", Map.of("indicator", indicators, "center", List.of("50%", "52%"), "radius", "60%",
                        "shape", "polygon",
                        "axisName", Map.of("color", "#475569"),
                        "splitArea", Map.of("areaStyle", Map.of("color", List.of("rgba(59,130,246,0.02)", "rgba(59,130,246,0.05)")))))
                .put("legend", Map.of("right", 0, "top", 28))
                .put("series", List.of(Map.of("type", "radar", "data", seriesData,
                        "symbol", "circle", "symbolSize", 6,
                        "lineStyle", Map.of("width", 2),
                        "areaStyle", Map.of("opacity", 0.15))))
                .build();
    }

    // ──────────────────────────────────────────────
    //  Common style helpers
    // ──────────────────────────────────────────────

    private OptionBuilder richBase(String title, String chartType) {
        return new OptionBuilder()
                .put("backgroundColor", "transparent")
                .put("color", COLORS)
                .put("title", Map.of("text", title, "left", 0,
                        "textStyle", Map.of("color", "#0f172a", "fontWeight", 700, "fontSize", 15)))
                .put("tooltip", Map.of("trigger", "axis", "axisPointer", Map.of("type", "shadow")))
                .put("grid", Map.of("bottom", 40, "left", 10, "top", 50, "right", 20, "containLabel", true))
                .put("animation", Map.of("duration", 800, "easing", "cubicOut"));
    }

    // ──────────────────────────────────────────────
    //  Data extraction helpers
    // ──────────────────────────────────────────────

    private List<String> extractCategories(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return List.of();
        List<String> keys = new ArrayList<>(data.get(0).keySet());
        String k = keys.get(0);
        return data.stream().map(r -> String.valueOf(r.getOrDefault(k, ""))).collect(Collectors.toList());
    }

    private List<Number> extractValues(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return List.of();
        List<String> keys = new ArrayList<>(data.get(0).keySet());
        String k = keys.get(keys.size() - 1);
        return data.stream().map(r -> toNumber(r.get(k))).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPieData(List<Map<String, Object>> data) {
        List<String> keys = new ArrayList<>(data.get(0).keySet());
        String nameKey = keys.get(0);
        String valKey = keys.size() > 1 ? keys.get(keys.size() - 1) : nameKey;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : data) {
            String name = String.valueOf(row.getOrDefault(nameKey, ""));
            Number v = toNumber(row.get(valKey));
            if (!name.isBlank() && v.doubleValue() > 0) result.add(Map.of("name", name, "value", v));
        }
        return result;
    }

    private Number toNumber(Object val) {
        if (val instanceof Number n) return n;
        if (val == null) return 0;
        try {
            String s = String.valueOf(val).trim().replaceAll("[,\\s]", "");
            if (s.contains(".")) return Double.parseDouble(s);
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getColor(int idx) {
        return COLORS.get(idx % COLORS.size());
    }

    // ──────────────────────────────────────────────
    //  HTML rendering (eg.html style)
    // ──────────────────────────────────────────────

    private String renderHtml(String reportTitle, String executiveSummary, String conclusionSummary,
                              List<String> keyFindings, List<RenderedChapter> chapters) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"utf-8\"/>\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        html.append("<title>").append(esc(reportTitle)).append("</title>\n");
        html.append("<style>\n").append(CSS).append("\n</style>\n");
        html.append("</head>\n<body>\n");
        html.append("<div class=\"topbar\"><div class=\"topbar-inner\">");
        html.append("<div class=\"topbar-title\">专利分析报告</div>");
        html.append("<div class=\"topbar-links\"><a href=\"#summary\">摘要</a>");
        if (keyFindings != null && !keyFindings.isEmpty()) {
            html.append("<a href=\"#findings\">发现</a>");
        }
        for (int i = 0; i < chapters.size(); i++) {
            html.append("<a href=\"#chapter-").append(i + 1).append("\">").append(i + 1).append("</a>");
        }
        if (conclusionSummary != null && !conclusionSummary.isBlank()) {
            html.append("<a href=\"#conclusion\">结论</a>");
        }
        html.append("</div></div></div>");
        html.append("<main>\n");
        html.append("<header class=\"report-header\">");
        html.append("<h1>").append(esc(reportTitle)).append("</h1>\n");
        html.append("<div class=\"meta\">生成时间：")
                .append(DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss").format(LocalDateTime.now()))
                .append("</div>\n");
        html.append("</header>");

        if (executiveSummary != null && !executiveSummary.isBlank()) {
            html.append("<section id=\"summary\">");
            html.append("<p class=\"summary\">").append(esc(executiveSummary)).append("</p>");
            html.append("</section>\n");
        }

        if (keyFindings != null && !keyFindings.isEmpty()) {
            html.append("<section id=\"findings\">");
            html.append("<h2>核心发现</h2>");
            html.append("<ul class=\"plain-list\">");
            for (String f : keyFindings) {
                html.append("<li>").append(esc(f)).append("</li>");
            }
            html.append("</ul></section>\n");
        }

        for (int i = 0; i < chapters.size(); i++) {
            RenderedChapter ch = chapters.get(i);
            html.append("<section id=\"chapter-").append(i + 1).append("\" class=\"report-section\">\n");
            html.append("<h2>").append(i + 1).append(". ").append(esc(ch.title)).append("</h2>\n");
            if (ch.objective != null && !ch.objective.isBlank()) {
                html.append("<p class=\"objective\">").append(esc(ch.objective)).append("</p>\n");
            }
            if (ch.analysisMarkdown != null && !ch.analysisMarkdown.isBlank()) {
                for (String p : ch.analysisMarkdown.split("\n\n+")) {
                    String t = p.trim();
                    if (!t.isEmpty()) {
                        html.append("<p>").append(esc(t)).append("</p>");
                    }
                }
            }
            if (ch.chartDataUrl != null && !ch.chartDataUrl.isBlank()) {
                html.append("<div class=\"chart-toolbar\"><h3>").append(esc(ch.title)).append("<span>图表</span></h3>");
                html.append("<button type=\"button\" class=\"chart-toggle\" data-toggle=\"chart\">隐藏图表</button></div>");
                html.append("<div class=\"chart-card\">");
                html.append("<img class=\"chart-image\" alt=\"").append(esc(ch.title)).append("\" src=\"")
                        .append(ch.chartDataUrl).append("\"/>");
                html.append("</div>");
            }
            if (ch.data != null && !ch.data.isEmpty()) {
                html.append("<div class=\"table-shell\">").append(buildTableHtml(ch.data)).append("</div>");
            }
            if (ch.keyFindings != null && !ch.keyFindings.isEmpty()) {
                html.append("<ul class=\"insight-list\">");
                for (String f : ch.keyFindings) {
                    html.append("<li>").append(esc(f)).append("</li>");
                }
                html.append("</ul>\n");
            }
            html.append("</section>\n");
        }

        if (conclusionSummary != null && !conclusionSummary.isBlank()) {
            html.append("<section id=\"conclusion\" class=\"report-section\">");
            html.append("<h2>总结与建议</h2>");
            for (String p : conclusionSummary.split("\n\n+")) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    html.append("<p>").append(esc(t)).append("</p>");
                }
            }
            html.append("</section>\n");
        }

        html.append("</main>\n");
        html.append("<script>");
        html.append("document.querySelectorAll('[data-toggle=\"chart\"]').forEach(btn=>btn.addEventListener('click',()=>{const card=btn.parentElement.nextElementSibling;const hidden=card.style.display==='none';card.style.display=hidden?'block':'none';btn.textContent=hidden?'隐藏图表':'显示图表';}));");
        html.append("</script>");
        html.append("</body>\n</html>");
        return html.toString();
    }

    private String buildTableHtml(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) return "";
        Set<String> headers = data.get(0).keySet();
        StringBuilder sb = new StringBuilder("<table>\n<thead><tr>\n");
        for (String h : headers) sb.append("  <th>").append(esc(translateField(h))).append("</th>\n");
        sb.append("</tr></thead>\n<tbody>\n");
        int n = Math.min(data.size(), 100);
        for (int i = 0; i < n; i++) {
            sb.append("<tr>\n");
            for (String h : headers) {
                Object v = data.get(i).get(h);
                String s = v != null ? String.valueOf(v) : "-";
                if (s.length() > 120) s = s.substring(0, 120) + "...";
                sb.append("  <td>").append(esc(s)).append("</td>\n");
            }
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n");
        return sb.toString();
    }

    private String simplifyForPdf(String html) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>");
        sb.append("<style>");
        sb.append("@page { size: A4; margin: 2cm; }");
        sb.append("body { font-family: serif; font-size: 12pt; line-height: 1.8; color: #333; }");
        sb.append("h1 { font-size: 20pt; text-align: center; }");
        sb.append("h2 { font-size: 16pt; margin-top: 24pt; border-top: 1px solid #ccc; padding-top: 12pt; }");
        sb.append("p { margin: 6pt 0; text-indent: 2em; }");
        sb.append("table { width: 100%; border-collapse: collapse; margin: 12pt 0; font-size: 10pt; }");
        sb.append("th, td { border: 1px solid #999; padding: 4pt 6pt; text-align: left; }");
        sb.append("th { background: #eee; }");
        sb.append(".meta, .objective { color: #666; font-style: italic; font-size: 10pt; }");
        sb.append(".summary { background: #f0f4ff; padding: 12pt; border-left: 4pt solid #2563eb; margin: 12pt 0; }");
        sb.append("ul { margin: 6pt 0; padding-left: 20pt; }");
        sb.append("li { margin: 3pt 0; }");
        sb.append("</style></head><body>");
        sb.append("<div style=\"max-width:800px;margin:0 auto;\">");
        int ms = html.indexOf("<main>");
        int me = html.indexOf("</main>");
        if (ms >= 0 && me > ms) {
            String c = html.substring(ms + 6, me);
            c = c.replaceAll("<div id=\"chart-\\d+\" class=\"chart\"></div>", "");
            c = c.replaceAll("<h3>.*?可视化</h3>", "");
            sb.append(c);
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private void ensureDir() throws Exception { Files.createDirectories(Paths.get(reportDir)); }

    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    // ──────────────────────────────────────────────
    //  Field name translation
    // ──────────────────────────────────────────────

    private String translateField(String name) {
        if (name == null) return "";
        return FIELD_NAMES.getOrDefault(name, name);
    }

    private static final Map<String, String> FIELD_NAMES = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("title", "标题"), Map.entry("college", "学院"),
            Map.entry("patent_type", "专利类型"), Map.entry("legal_status", "法律状态"),
            Map.entry("application_year", "申请年"), Map.entry("application_date", "申请日"),
            Map.entry("grant_year", "授权年"), Map.entry("patent_value", "专利价值"),
            Map.entry("technical_value", "技术价值"), Map.entry("market_value", "市场价值"),
            Map.entry("cited_patents", "被引用数"), Map.entry("cited_in_5_years", "5年引用数"),
            Map.entry("claims_count", "权利要求数"), Map.entry("inventor_count", "发明人数"),
            Map.entry("ipc_main_class", "IPC主分类"), Map.entry("ipc_main_class_interpretation", "IPC分类释义"),
            Map.entry("inventors", "发明人"), Map.entry("inventor", "发明人"),
            Map.entry("current_assignee", "当前专利权人"), Map.entry("original_assignee", "原始专利权人"),
            Map.entry("agency", "代理机构"), Map.entry("count", "数量"),
            Map.entry("patent_count", "专利数量"), Map.entry("total_patents", "专利总数"),
            Map.entry("college_count", "学院数"), Map.entry("application_count", "申请量"),
            Map.entry("grant_count", "授权量"), Map.entry("avg_value", "平均价值"),
            Map.entry("total", "总计"), Map.entry("name", "名称"), Map.entry("value", "值"),
            Map.entry("id", "编号"), Map.entry("avg_score", "平均评分"),
            Map.entry("grant_rate", "授权率"), Map.entry("year_span", "年份跨度"),
            Map.entry("type_count", "类型数"), Map.entry("agent_count", "代理数量"),
            Map.entry("technical_field", "技术领域"), Map.entry("license_count", "许可次数"),
            Map.entry("license_type", "许可类型"), Map.entry("transferor", "转让人"),
            Map.entry("transferee", "受让人"), Map.entry("current_assignee_province", "省份"),
            Map.entry("current_assignee_type", "权利人类型")
    ));

    // ──────────────────────────────────────────────
    //  DOCX helpers
    // ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void addDocxHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        p.setSpacingAfter(40);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(14);
        r.setFontFamily(DOCX_FONT_FAMILY);
        r.setColor("1e40af");
    }

    private void addDocxSectionTitle(XWPFDocument doc, String title, String subtitle) {
        XWPFParagraph badgeP = doc.createParagraph();
        badgeP.setSpacingBefore(120);
        badgeP.setSpacingAfter(18);
        XWPFRun badgeR = badgeP.createRun();
        badgeR.setText(subtitle);
        badgeR.setBold(true);
        badgeR.setFontSize(9);
        badgeR.setFontFamily(DOCX_FONT_FAMILY);
        badgeR.setColor("2563eb");

        XWPFParagraph titleP = doc.createParagraph();
        titleP.setSpacingAfter(60);
        XWPFRun titleR = titleP.createRun();
        titleR.setText(title);
        titleR.setBold(true);
        titleR.setFontSize(16);
        titleR.setFontFamily(DOCX_FONT_FAMILY);
        titleR.setColor("0f172a");
    }

    private void addDocxSpacer(XWPFDocument doc, int spacingAfter) {
        XWPFParagraph spacer = doc.createParagraph();
        spacer.setSpacingAfter(spacingAfter);
    }

    @SuppressWarnings("unchecked")
    private void buildDocxTable(XWPFDocument doc, List<Map<String, Object>> data) {
        if (data.isEmpty()) return;
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        int n = Math.min(data.size(), 100);
        XWPFTable t = doc.createTable(n + 1, headers.size());
        t.setWidth("100%");
        for (int c = 0; c < headers.size(); c++) {
            XWPFTableCell cell = t.getRow(0).getCell(c);
            cell.setColor("EAF2FF");
            XWPFParagraph paragraph = cell.getParagraphs().get(0);
            paragraph.setSpacingAfter(0);
            XWPFRun r = paragraph.createRun();
            r.setText(translateField(headers.get(c)));
            r.setBold(true);
            r.setFontSize(10);
            r.setFontFamily(DOCX_FONT_FAMILY);
            r.setColor("1E3A8A");
        }
        for (int rw = 0; rw < n; rw++) {
            for (int c = 0; c < headers.size(); c++) {
                Object v = data.get(rw).get(headers.get(c));
                String s = v != null ? String.valueOf(v) : "-";
                if (s.length() > 100) s = s.substring(0, 100) + "...";
                XWPFTableCell cell = t.getRow(rw + 1).getCell(c);
                if (rw % 2 == 1) {
                    cell.setColor("F8FBFF");
                }
                XWPFParagraph paragraph = cell.getParagraphs().get(0);
                paragraph.setSpacingAfter(0);
                XWPFRun run = paragraph.createRun();
                run.setText(s);
                run.setFontSize(9);
                run.setFontFamily(DOCX_FONT_FAMILY);
                run.setColor("334155");
            }
        }
        addDocxSpacer(doc, 80);
    }

    //  Inner types
    // ──────────────────────────────────────────────

    public record ChapterData(String title, String objective, String analysisMarkdown,
                               Map<String, Object> chartOption, List<Map<String, Object>> data,
                               List<String> keyFindings) {}

    private record RenderedChapter(String title, String objective, String analysisMarkdown,
                                   Map<String, Object> chartOption, String chartDataUrl,
                                   List<Map<String, Object>> data, List<String> keyFindings) {}

    public record ReportBuildResult(String htmlPath, String pdfPath, String docxPath) {}

    private static class OptionBuilder {
        private final Map<String, Object> map = new LinkedHashMap<>();
        OptionBuilder put(String key, Object val) { map.put(key, val); return this; }
        Map<String, Object> build() { return map; }
    }

    private static final List<String> COLORS = List.of(
            "#3b82f6", "#0ea5e9", "#10b981", "#f59e0b", "#ef4444",
            "#8b5cf6", "#06b6d4", "#84cc16", "#f97316", "#ec4899",
            "#6366f1", "#14b8a6", "#d946ef", "#eab308", "#22c55e"
    );

    private static final String CSS = """
            * { margin:0; padding:0; box-sizing:border-box; }
            @page { size:A4; margin:16mm 14mm; }
            :root {
                --bg:#f8fafc;
                --paper:#ffffff;
                --ink:#0f172a;
                --muted:#64748b;
                --line:#e2e8f0;
                --accent:#2563eb;
                --summary:#eff6ff;
            }
            html { scroll-behavior:smooth; }
            body { background:var(--bg); color:var(--ink); font-family:"Microsoft YaHei","PingFang SC",Arial,sans-serif; }
            .topbar { position:sticky; top:0; z-index:10; background:rgba(248,250,252,.92); backdrop-filter:blur(10px); border-bottom:1px solid var(--line); }
            .topbar-inner { max-width:1080px; margin:0 auto; padding:12px 36px; display:flex; justify-content:space-between; gap:16px; align-items:center; }
            .topbar-title { font-size:12px; letter-spacing:.12em; text-transform:uppercase; color:var(--muted); }
            .topbar-links { display:flex; gap:14px; flex-wrap:wrap; }
            .topbar-links a { color:var(--muted); text-decoration:none; font-size:13px; }
            .topbar-links a:hover { color:var(--accent); }
            main { max-width:1080px; margin:0 auto; padding:48px 36px 64px; background:var(--paper); }
            .report-header h1 { margin:0 0 10px; font-size:30px; line-height:1.35; }
            .meta, .objective { color:var(--muted); }
            .meta { font-size:14px; margin-bottom:16px; }
            .summary { padding:18px 20px; background:var(--summary); border-left:4px solid var(--accent); font-size:14px; line-height:1.9; }
            h2 { margin-top:42px; padding-top:22px; border-top:1px solid var(--line); font-size:22px; }
            h3 { margin-top:24px; font-size:16px; display:flex; justify-content:space-between; align-items:center; gap:12px; }
            h3 span { color:var(--muted); font-size:12px; font-weight:400; }
            p, li { font-size:14px; line-height:1.9; }
            .report-section p { margin-top:14px; }
            .objective { margin-top:12px; }
            .plain-list, .insight-list { margin-top:14px; padding-left:22px; }
            .chart-toolbar { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-top:18px; }
            .chart-toggle { appearance:none; border:1px solid var(--line); background:#fff; color:var(--muted); border-radius:999px; padding:6px 12px; font:inherit; font-size:12px; cursor:pointer; }
            .chart-toggle:hover { color:var(--accent); border-color:var(--accent); }
            .chart-card { margin:18px 0; padding:14px; background:#fff; border:1px solid var(--line); }
            .chart-image { display:block; width:100%; }
            .table-shell { overflow:auto; }
            table { width:100%; border-collapse:collapse; margin-top:16px; font-size:12px; }
            th, td { border:1px solid var(--line); padding:8px 10px; text-align:left; vertical-align:top; }
            th { background:#f1f5f9; }
            @media (max-width: 900px) {
                .topbar-inner { padding:12px 16px; align-items:flex-start; flex-direction:column; }
                main { padding:24px 16px 40px; }
            }
            @media print {
                body { background:#fff; }
                .topbar { display:none; }
                main { padding:20px; }
                .chart-card { break-inside:avoid; }
            }
            """;
}

