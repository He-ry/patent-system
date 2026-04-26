package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "统计请求")
public class StatisticsRequest {

    @Schema(description = "统计字段", required = true, example = "patentType")
    private String field;

    @Schema(description = "返回条数限制", example = "10")
    private Integer limit = 10;
}
