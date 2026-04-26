package com.example.patent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "词云请求")
public class WordCloudRequest {

    @Schema(description = "分析维度: title(标题), college(学院), ipcMainClass(IPC主分类), ipcMainClassInterpretation(IPC部释义), inventors(发明人), applicationFieldClassification(应用领域), technicalSubjectClassification(技术主题), technicalFields(技术领域)", example = "title")
    private String dimension = "title";

    @Schema(description = "返回词数", example = "100")
    private Integer limit = 100;

    @Schema(description = "专利类型过滤")
    private String patentType;

    @Schema(description = "法律状态过滤")
    private String legalStatus;

    @Schema(description = "申请年份过滤")
    private String applicationYear;
}
