package com.example.patent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("patent_info")
public class PatentInfo {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String serialNumber;
    private String title;
    private Date applicationDate;
    private Integer inventorCount;
    private String college;
    private String legalStatus;
    private String patentType;
    private String applicationNumber;
    private Date grantDate;
    private String currentAssignee;
    private String originalAssignee;
    private String agency;
    private String currentAssigneeProvince;
    private String originalAssigneeProvince;
    private String originalAssigneeType;
    private String currentAssigneeType;
    private Integer applicationYear;
    private Date publicationDate;
    private Integer grantYear;
    private String ipcMainClass;
    @TableField(exist = false)
    private String ipcMainClassInterpretation;
    @TableField(exist = false)
    private String technicalProblem;
    @TableField(exist = false)
    private String technicalEffect;
    @TableField(exist = false)
    private String strategicIndustryClassification;
    @TableField(exist = false)
    private String applicationFieldClassification;
    @TableField(exist = false)
    private String technicalSubjectClassification;
    private Date expiryDate;
    private Integer simpleFamilyCitedPatents;
    private Integer citedPatents;
    @TableField("cited_in_5_years")
    private Integer citedIn5Years;
    private Integer claimsCount;
    private BigDecimal patentValue;
    private BigDecimal technicalValue;
    private BigDecimal marketValue;
    private Date transferEffectiveDate;
    private String licenseType;
    private Integer licenseCount;
    private Date licenseEffectiveDate;
    private String transferor;
    private String transferee;

    private Date createTime;
    private Date updateTime;
}
