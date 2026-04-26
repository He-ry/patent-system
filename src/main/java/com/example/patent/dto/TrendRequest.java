package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "趋势分析请求")
public class TrendRequest {

    @Schema(description = "分析维度: college(学院), ipcMainClassInterpretation(IPC部释义), inventors(发明人), applicationFieldClassification(应用领域), technicalSubjectClassification(技术主题)", example = "college")
    private String dimension = "college";

    @Schema(description = "返回条数限制", example = "10")
    private Integer limit = 10;

    @Schema(description = "专利类型过滤")
    private String patentType;

    @Schema(description = "法律状态过滤")
    private String legalStatus;

    @Schema(description = "开始年份")
    private String startYear;

    @Schema(description = "结束年份")
    private String endYear;
}
