package com.example.patent.service;

import com.example.patent.dto.*;
import com.example.patent.entity.PatentInfo;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface PatentService {

    void addPatent(PatentAddRequest request);

    void updatePatent(PatentAddRequest request);

    void deletePatent(String id);

    PageResponse<PatentListResponse> getPatentList(PatentQueryRequest request);

    PatentListResponse getPatentDetail(String id);

    List<Map<String, Object>> getStatistics(String field, Integer limit);

    ImportResult importPatents(List<PatentImportData> dataList);

    void exportPatents(PatentQueryRequest request, HttpServletResponse response);

    List<WordCloudResponse> getWordCloud(WordCloudRequest request);

    List<TrendResponse> getTrend(TrendRequest request);
}
