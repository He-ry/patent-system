package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "趋势分析响应")
public class TrendResponse {

    @Schema(description = "年份")
    private String year;

    @Schema(description = "维度值")
    private String name;

    @Schema(description = "数量")
    private Integer count;
}
