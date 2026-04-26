package com.example.patent.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class PatentImportData {

    @ExcelProperty("序号")
    private String serialNumber;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("申请号")
    private String applicationNumber;

    @ExcelProperty("申请日")
    private String applicationDate;

    @ExcelProperty("授权日")
    private String grantDate;

    @ExcelProperty("专利类型")
    private String patentType;

    @ExcelProperty("法律状态/事件")
    private String legalStatus;

    @ExcelProperty("学院")
    private String college;

    @ExcelProperty("[标]当前申请(专利权)人")
    private String currentAssignee;

    @ExcelProperty("[标]原始申请(专利权)人")
    private String originalAssignee;

    @ExcelProperty("发明人")
    private String inventors;

    @ExcelProperty("技术领域")
    private String technicalFields;

    @ExcelProperty("[标]技术问题短语")
    private String technicalProblem;

    @ExcelProperty("[标]技术功效短语")
    private String technicalEffect;

    @ExcelProperty("IPC分类号")
    private String ipcClassifications;

    @ExcelProperty("申请年")
    private String applicationYear;

    @ExcelProperty("CPC分类号IPC")
    private String cpcClassifications;

    @ExcelProperty("发明人数量")
    private String inventorCount;

    @ExcelProperty("[标]代理机构")
    private String agency;

    @ExcelProperty("当前申请(专利权)人州/省")
    private String currentAssigneeProvince;

    @ExcelProperty("原始申请(专利权)人州/省")
    private String originalAssigneeProvince;

    @ExcelProperty("[标]原始申请(专利权)人类型")
    private String originalAssigneeType;

    @ExcelProperty("[标]当前申请(专利权)人类型")
    private String currentAssigneeType;

    @ExcelProperty("公开(公告)日")
    private String publicationDate;

    @ExcelProperty("授权年")
    private String grantYear;

    @ExcelProperty("IPC主分类号")
    private String ipcMainClass;

    @ExcelProperty("IPC主分类号(部)释义")
    private String ipcMainClassInterpretation;

    @ExcelProperty("战略新兴产业分类")
    private String strategicIndustryClassification;

    @ExcelProperty("应用领域分类")
    private String applicationFieldClassification;

    @ExcelProperty("技术主题分类")
    private String technicalSubjectClassification;

    @ExcelProperty("失效日")
    private String expiryDate;

    @ExcelProperty("简单同族被引用专利总数")
    private String simpleFamilyCitedPatents;

    @ExcelProperty("被引用专利数量")
    private String citedPatents;

    @ExcelProperty("5年内被引用数量")
    private String citedIn5Years;

    @ExcelProperty("权利要求数")
    private String claimsCount;

    @ExcelProperty("专利价值")
    private String patentValue;

    @ExcelProperty("技术价值")
    private String technicalValue;

    @ExcelProperty("市场价值")
    private String marketValue;

    @ExcelProperty("权利转移生效日")
    private String transferEffectiveDate;

    @ExcelProperty("许可类型")
    private String licenseType;

    @ExcelProperty("许可次数")
    private String licenseCount;

    @ExcelProperty("许可生效日")
    private String licenseEffectiveDate;

    @ExcelProperty("转让人")
    private String transferor;

    @ExcelProperty("受让人")
    private String transferee;
}
