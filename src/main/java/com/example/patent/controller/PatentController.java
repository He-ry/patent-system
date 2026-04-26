package com.example.patent.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.example.patent.common.Result;
import com.example.patent.dto.*;
import com.example.patent.entity.PatentInfo;
import com.example.patent.service.PatentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patent")
@Tag(name = "专利管理", description = "专利信息管理接口")
public class PatentController {

    @Autowired
    private PatentService patentService;

    @PostMapping("/add")
    @Operation(summary = "添加专利", description = "添加新的专利信息")
    public Result<Void> addPatent(@RequestBody PatentAddRequest request) {
        patentService.addPatent(request);
        return Result.success();
    }

    @PutMapping("/update")
    @Operation(summary = "更新专利", description = "根据ID更新专利信息")
    public Result<Void> updatePatent(@RequestBody PatentAddRequest request) {
        if (request.getId() == null || request.getId().isEmpty()) {
            return Result.error("专利ID不能为空");
        }
        patentService.updatePatent(request);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除专利", description = "根据ID删除专利")
    public Result<Void> deletePatent(
            @Parameter(description = "专利ID") @PathVariable String id) {
        patentService.deletePatent(id);
        return Result.success();
    }

    @PostMapping("/list")
    @Operation(summary = "获取专利列表", description = "分页查询专利列表")
    public Result<PageResponse<PatentListResponse>> getPatentList(@RequestBody PatentQueryRequest request) {
        PageResponse<PatentListResponse> response = patentService.getPatentList(request);
        return Result.success(response);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取专利详情", description = "根据ID获取专利详细信息")
    public Result<PatentListResponse> getPatentDetail(@PathVariable String id) {
        PatentListResponse response = patentService.getPatentDetail(id);
        if (response == null) {
            return Result.error("专利不存在");
        }
        return Result.success(response);
    }

    @PostMapping("/statistics")
    @Operation(summary = "统计专利数量", description = "根据指定字段统计专利数量，支持返回指定条数")
    public Result<List<Map<String, Object>>> getStatistics(@RequestBody StatisticsRequest request) {
        List<Map<String, Object>> result = patentService.getStatistics(request.getField(), request.getLimit());
        return Result.success(result);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入专利", description = "通过Excel文件批量导入专利数据")
    public Result<ImportResult> importPatents(
            @Parameter(description = "Excel文件") @RequestPart("file") MultipartFile file) {

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.error("请上传Excel文件（.xlsx或.xls）");
        }

        try {
            List<PatentImportData> dataList = new ArrayList<>();

            EasyExcel.read(file.getInputStream(), PatentImportData.class, new AnalysisEventListener<PatentImportData>() {
                @Override
                public void invoke(PatentImportData data, AnalysisContext context) {
                    dataList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();

            ImportResult result = patentService.importPatents(dataList);
            return Result.success("导入完成", result);
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        }
    }

    @PostMapping("/export")
    @Operation(summary = "导出专利", description = "导出专利数据到Excel文件")
    public void exportPatents(@RequestBody PatentQueryRequest request, HttpServletResponse response) {
        patentService.exportPatents(request, response);
    }

    @PostMapping("/wordcloud")
    @Operation(summary = "获取词云数据", description = "根据维度获取词云统计数据")
    public Result<List<WordCloudResponse>> getWordCloud(@RequestBody WordCloudRequest request) {
        List<WordCloudResponse> result = patentService.getWordCloud(request);
        return Result.success(result);
    }

    @PostMapping("/trend")
    @Operation(summary = "获取趋势分析数据", description = "根据维度获取随年份变化的趋势数据")
    public Result<List<TrendResponse>> getTrend(@RequestBody TrendRequest request) {
        List<TrendResponse> result = patentService.getTrend(request);
        return Result.success(result);
    }
}
