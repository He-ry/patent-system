package com.example.patent.entity;

import java.util.Arrays;
import java.util.Optional;

public enum PatentSplitFieldType {
    INVENTOR("inventor", "\\|", "发明人"),
    TECHNICAL_FIELD("technical_field", "\\|", "技术领域"),
    TECHNICAL_PROBLEM("technical_problem", "\\|", "[标]技术问题短语"),
    TECHNICAL_EFFECT("technical_effect", "\\|", "[标]技术功效短语"),
    IPC_CLASSIFICATION("ipc_classification", "\\|", "IPC分类号"),
    CPC_CLASSIFICATION("cpc_classification", "\\|", "CPC分类号"),
    TECHNICAL_SUBJECT_CLASSIFICATION("technical_subject_classification", "\\|", "技术主题分类"),
    APPLICATION_FIELD_CLASSIFICATION("application_field_classification", "\\|", "应用领域分类"),
    IPC_MAIN_CLASS_INTERPRETATION("ipc_main_class_interpretation", "[;；]", "IPC主分类号(部)释义"),
    STRATEGIC_INDUSTRY_CLASSIFICATION("strategic_industry_classification", "、", "战略新兴产业分类");

    private final String code;
    private final String delimiterRegex;
    private final String label;

    PatentSplitFieldType(String code, String delimiterRegex, String label) {
        this.code = code;
        this.delimiterRegex = delimiterRegex;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String delimiterRegex() {
        return delimiterRegex;
    }

    public String label() {
        return label;
    }

    public static Optional<PatentSplitFieldType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }
}
