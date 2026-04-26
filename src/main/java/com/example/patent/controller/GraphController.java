package com.example.patent.controller;

import com.example.patent.common.Result;
import com.example.patent.dto.GraphDataResponse;
import com.example.patent.dto.GraphOverviewResponse;
import com.example.patent.service.GraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@Tag(name = "知识图谱 2.0", description = "面向关系分析的新知识图谱接口")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    @PostMapping("/sync")
    @Operation(summary = "全量重建图谱")
    public Result<Map<String, Object>> syncToGraph() {
        return Result.success(graphService.syncAllPatentsToGraph());
    }

    @PostMapping("/sync/incremental")
    @Operation(summary = "增量同步图谱", description = "不清空现有图谱，只对事实层执行 upsert，并重建分析层")
    public Result<Map<String, Object>> syncIncremental() {
        return Result.success(graphService.syncIncremental());
    }

    @PostMapping("/rebuild/analysis")
    @Operation(summary = "重建分析层")
    public Result<Map<String, Object>> rebuildAnalysis() {
        return Result.success(graphService.rebuildAnalysisLayer());
    }

    @PostMapping("/rebuild/stats")
    @Operation(summary = "刷新图谱统计属性")
    public Result<Map<String, Object>> rebuildStats() {
        return Result.success(graphService.refreshStats());
    }

    @GetMapping("/overview")
    @Operation(summary = "获取图谱总览")
    public Result<GraphOverviewResponse> getOverview() {
        return Result.success(graphService.getGraphOverview());
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取图谱统计", description = "兼容旧页面，返回新图谱总览统计")
    public Result<GraphOverviewResponse> getStatistics() {
        return Result.success(graphService.getGraphOverview());
    }

    @GetMapping("/inventor/{name}")
    @Operation(summary = "获取发明人关系视图")
    public Result<GraphDataResponse> getInventorGraph(@PathVariable String name,
                                                      @RequestParam(required = false) Integer limit) {
        return Result.success(graphService.getInventorGraph(name, limit));
    }

    @GetMapping("/topic/{name}")
    @Operation(summary = "获取技术主题关系视图")
    public Result<GraphDataResponse> getTopicGraph(@PathVariable String name,
                                                   @RequestParam(required = false) Integer limit) {
        return Result.success(graphService.getTopicGraph(name, limit));
    }

    @GetMapping("/network/co-inventor")
    @Operation(summary = "获取发明人协作网络")
    public Result<GraphDataResponse> getCoInventorNetwork(
            @RequestParam(required = false) Integer minWeight,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String keyword) {
        return Result.success(graphService.getCoInventorNetwork(minWeight, limit, keyword));
    }

    @GetMapping("/network/topic-cooccurrence")
    @Operation(summary = "获取技术主题共现网络")
    public Result<GraphDataResponse> getTopicCooccurrence(
            @RequestParam(required = false) Integer minWeight,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String keyword) {
        return Result.success(graphService.getTopicCooccurrence(minWeight, limit, keyword));
    }

    @GetMapping("/network/high-value")
    @Operation(summary = "获取高价值专利网络")
    public Result<GraphDataResponse> getHighValueNetwork(
            @RequestParam(required = false) Double minPatentValue,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String keyword) {
        return Result.success(graphService.getHighValueNetwork(minPatentValue, limit, keyword));
    }

    @GetMapping("/path")
    @Operation(summary = "获取实体最短路径")
    public Result<GraphDataResponse> getShortestPath(
            @RequestParam String fromType,
            @RequestParam String fromId,
            @RequestParam String toType,
            @RequestParam String toId,
            @RequestParam(required = false) Integer maxDepth) {
        return Result.success(graphService.getShortestPath(fromType, fromId, toType, toId, maxDepth));
    }

    @GetMapping("/search")
    @Operation(summary = "统一搜索图谱实体")
    public Result<List<Map<String, Object>>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit) {
        return Result.success(graphService.search(keyword, type, limit));
    }

    @PostMapping("/patent-titles")
    @Operation(summary = "批量查询专利标题")
    public Result<List<Map<String, Object>>> getPatentTitles(@RequestBody List<String> patentIds) {
        return Result.success(graphService.getPatentTitles(patentIds));
    }
}
