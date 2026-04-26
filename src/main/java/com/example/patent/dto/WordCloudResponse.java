package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "词云数据响应")
public class WordCloudResponse {

    @Schema(description = "词语")
    private String word;

    @Schema(description = "出现频次")
    private Integer count;
}
