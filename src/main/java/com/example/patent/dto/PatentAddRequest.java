package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "添加专利请求")
public class PatentAddRequest {

    @Schema(description = "专利ID（更新时必填）")
    private String id;

    @Schema(description = "专利名称", required = true)
    private String title;

    @Schema(description = "序号")
    private String serialNumber;

    @Schema(description = "申请号")
    private String applicationNumber;

    @Schema(description = "申请日期")
    private Date applicationDate;

    @Schema(description = "专利类型")
    private String patentType;

    @Schema(description = "法律状态/事件")
    private String legalStatus;

    @Schema(description = "学院")
    private String college;

    @Schema(description = "当前申请(专利权)人")
    private String currentAssignee;

    @Schema(description = "原始申请(专利权)人")
    private String originalAssignee;

    @Schema(description = "申请年份")
    private String applicationYear;

    @Schema(description = "公开(公告)日")
    private Date publicationDate;

    @Schema(description = "授权日")
    private Date grantDate;

    @Schema(description = "授权年")
    private String grantYear;

    @Schema(description = "IPC主分类号")
    private String ipcMainClass;

    @Schema(description = "IPC主分类号(部)释义")
    private String ipcMainClassInterpretation;

    @Schema(description = "发明人（多个用|分隔）")
    private String inventors;

    @Schema(description = "技术领域（多个用|分隔）")
    private String technicalFields;

    @Schema(description = "IPC分类号（多个用|分隔）")
    private String ipcClassifications;

    @Schema(description = "CPC分类号（多个用|分隔）")
    private String cpcClassifications;

    @Schema(description = "[标]技术问题短语")
    private String technicalProblem;

    @Schema(description = "[标]技术功效短语")
    private String technicalEffect;

    @Schema(description = "发明人数量")
    private String inventorCount;

    @Schema(description = "[标]代理机构")
    private String agency;

    @Schema(description = "当前申请(专利权)人州/省")
    private String currentAssigneeProvince;

    @Schema(description = "原始申请(专利权)人州/省")
    private String originalAssigneeProvince;

    @Schema(description = "[标]原始申请(专利权)人类型")
    private String originalAssigneeType;

    @Schema(description = "[标]当前申请(专利权)人类型")
    private String currentAssigneeType;

    @Schema(description = "战略新兴产业分类")
    private String strategicIndustryClassification;

    @Schema(description = "应用领域分类")
    private String applicationFieldClassification;

    @Schema(description = "技术主题分类")
    private String technicalSubjectClassification;

    @Schema(description = "失效日")
    private Date expiryDate;

    @Schema(description = "简单同族被引用专利总数")
    private String simpleFamilyCitedPatents;

    @Schema(description = "被引用专利数量")
    private String citedPatents;

    @Schema(description = "5年内被引用数量")
    private String citedIn5Years;

    @Schema(description = "权利要求数")
    private String claimsCount;

    @Schema(description = "专利价值")
    private String patentValue;

    @Schema(description = "技术价值")
    private String technicalValue;

    @Schema(description = "市场价值")
    private String marketValue;

    @Schema(description = "权利转移生效日")
    private Date transferEffectiveDate;

    @Schema(description = "许可类型")
    private String licenseType;

    @Schema(description = "许可次数")
    private String licenseCount;

    @Schema(description = "许可生效日")
    private Date licenseEffectiveDate;

    @Schema(description = "转让人")
    private String transferor;

    @Schema(description = "受让人")
    private String transferee;
}