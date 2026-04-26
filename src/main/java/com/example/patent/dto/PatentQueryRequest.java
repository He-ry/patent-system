package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "专利查询请求")
public class PatentQueryRequest {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer size = 10;

    @Schema(description = "关键词搜索")
    private String keyword;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "申请号")
    private String applicationNumber;

    @Schema(description = "专利类型")
    private String patentType;

    @Schema(description = "法律状态")
    private String legalStatus;

    @Schema(description = "申请年份")
    private String applicationYear;

    @Schema(description = "学院过滤")
    private String college;

    @Schema(description = "IPC主分类号(部)释义过滤")
    private String ipcMainClassInterpretation;

    @Schema(description = "发明人过滤")
    private String inventors;

    @Schema(description = "技术领域")
    private String technicalFields;

    @Schema(description = "技术问题")
    private String technicalProblem;

    @Schema(description = "技术功效")
    private String technicalEffect;

    @Schema(description = "IPC分类号")
    private String ipcClassifications;

    @Schema(description = "CPC分类号")
    private String cpcClassifications;

    @Schema(description = "应用领域分类过滤")
    private String applicationFieldClassification;

    @Schema(description = "技术主题分类过滤")
    private String technicalSubjectClassification;

    @Schema(description = "战略新兴产业分类")
    private String strategicIndustryClassification;
}
