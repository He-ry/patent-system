package com.example.patent.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "专利列表响应")
public class PatentListResponse {

    @Schema(description = "专利ID")
    private String id;

    @ExcelProperty("序号")
    @ColumnWidth(10)
    private String serialNumber;

    @ExcelProperty("标题")
    @ColumnWidth(30)
    private String title;

    @ExcelProperty("申请日")
    @ColumnWidth(15)
    private String applicationDate;

    @ExcelProperty("发明人")
    @ColumnWidth(30)
    private String inventors;

    @ExcelProperty("学院")
    @ColumnWidth(20)
    private String college;

    @ExcelProperty("法律状态/事件")
    @ColumnWidth(15)
    private String legalStatus;

    @ExcelProperty("专利类型")
    @ColumnWidth(12)
    private String patentType;

    @ExcelProperty("申请号")
    @ColumnWidth(20)
    private String applicationNumber;

    @ExcelProperty("授权日")
    @ColumnWidth(15)
    private String grantDate;

    @ExcelProperty("技术领域")
    @ColumnWidth(30)
    private String technicalFields;

    @ExcelProperty("[标]技术问题短语")
    @ColumnWidth(30)
    private String technicalProblem;

    @ExcelProperty("[标]技术功效短语")
    @ColumnWidth(30)
    private String technicalEffect;

    @ExcelProperty("[标]当前申请(专利权)人")
    @ColumnWidth(25)
    private String currentAssignee;

    @ExcelProperty("[标]原始申请(专利权)人")
    @ColumnWidth(25)
    private String originalAssignee;

    @ExcelProperty("发明人数量")
    @ColumnWidth(12)
    private String inventorCount;

    @ExcelProperty("[标]代理机构")
    @ColumnWidth(20)
    private String agency;

    @ExcelProperty("当前申请(专利权)人州/省")
    @ColumnWidth(20)
    private String currentAssigneeProvince;

    @ExcelProperty("原始申请(专利权)人州/省")
    @ColumnWidth(20)
    private String originalAssigneeProvince;

    @ExcelProperty("[标]原始申请(专利权)人类型")
    @ColumnWidth(20)
    private String originalAssigneeType;

    @ExcelProperty("[标]当前申请(专利权)人类型")
    @ColumnWidth(20)
    private String currentAssigneeType;

    @ExcelProperty("申请年")
    @ColumnWidth(10)
    private String applicationYear;

    @ExcelProperty("公开(公告)日")
    @ColumnWidth(15)
    private String publicationDate;

    @ExcelProperty("授权年")
    @ColumnWidth(10)
    private String grantYear;

    @ExcelProperty("IPC分类号")
    @ColumnWidth(25)
    private String ipcClassifications;

    @ExcelProperty("CPC分类号")
    @ColumnWidth(25)
    private String cpcClassifications;

    @ExcelProperty("IPC主分类号")
    @ColumnWidth(20)
    private String ipcMainClass;

    @ExcelProperty("IPC主分类号(部)释义")
    @ColumnWidth(25)
    private String ipcMainClassInterpretation;

    @ExcelProperty("战略新兴产业分类")
    @ColumnWidth(25)
    private String strategicIndustryClassification;

    @ExcelProperty("应用领域分类")
    @ColumnWidth(25)
    private String applicationFieldClassification;

    @ExcelProperty("技术主题分类")
    @ColumnWidth(25)
    private String technicalSubjectClassification;

    @ExcelProperty("失效日")
    @ColumnWidth(15)
    private String expiryDate;

    @ExcelProperty("被引用专利数量")
    @ColumnWidth(15)
    private String citedPatents;

    @ExcelProperty("5年内被引用数量")
    @ColumnWidth(15)
    private String citedIn5Years;

    @ExcelProperty("权利要求数")
    @ColumnWidth(12)
    private String claimsCount;

    @ExcelProperty("专利价值")
    @ColumnWidth(12)
    private String patentValue;

    @ExcelProperty("技术价值")
    @ColumnWidth(12)
    private String technicalValue;

    @ExcelProperty("市场价值")
    @ColumnWidth(12)
    private String marketValue;

    @ExcelProperty("权利转移生效日")
    @ColumnWidth(15)
    private String transferEffectiveDate;

    @ExcelProperty("许可类型")
    @ColumnWidth(15)
    private String licenseType;

    @ExcelProperty("许可次数")
    @ColumnWidth(12)
    private String licenseCount;

    @ExcelProperty("许可生效日")
    @ColumnWidth(15)
    private String licenseEffectiveDate;

    @ExcelProperty("转让人")
    @ColumnWidth(20)
    private String transferor;

    @ExcelProperty("受让人")
    @ColumnWidth(20)
    private String transferee;
}
