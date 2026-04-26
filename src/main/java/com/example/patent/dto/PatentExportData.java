package com.example.patent.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
@ColumnWidth(20)
public class PatentExportData {

    @ExcelProperty("申请号")
    private String applicationNumber;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("高校")
    private String college;

    @ExcelProperty("专利类型")
    private String patentType;

    @ExcelProperty("法律状态")
    private String legalStatus;

    @ExcelProperty("申请日期")
    private String applicationDate;

    @ExcelProperty("申请年份")
    private String applicationYear;

    @ExcelProperty("当前权利人")
    private String currentAssignee;

    @ExcelProperty("原始权利人")
    private String originalAssignee;

    @ExcelProperty("IPC主分类号")
    private String ipcMainClass;

    @ExcelProperty("发明人数量")
    private String inventorCount;

    @ExcelProperty("代理机构")
    private String agency;

    @ExcelProperty("授权日期")
    private String grantDate;

    @ExcelProperty("授权年份")
    private String grantYear;

    @ExcelProperty("专利价值")
    private String patentValue;

    @ExcelProperty("技术价值")
    private String technicalValue;

    @ExcelProperty("市场价值")
    private String marketValue;

    @ExcelProperty("被引用专利数量")
    private String citedPatents;

    @ExcelProperty("权利要求数")
    private String claimsCount;

    @ExcelProperty("转让人")
    private String transferor;

    @ExcelProperty("受让人")
    private String transferee;
}
