package com.example.patent.entity;

import lombok.Data;

@Data
public class PatentInfoField {
    private String id;
    private String patentId;
    private String fieldType;
    private String fieldValue;
    private Integer seq;
}
