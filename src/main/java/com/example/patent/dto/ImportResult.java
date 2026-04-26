package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "导入结果")
public class ImportResult {
    
    @Schema(description = "总数")
    private int total;
    
    @Schema(description = "成功数")
    private int success;
    
    @Schema(description = "跳过数")
    private int skipped;
    
    @Schema(description = "重复的申请号列表")
    private List<String> duplicates = new ArrayList<>();
}
