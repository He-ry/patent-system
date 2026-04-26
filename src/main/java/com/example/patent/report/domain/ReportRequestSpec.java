package com.example.patent.report.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestSpec {
    @Builder.Default
    private String rawQuery = "";
    private String titleHint;
    private String scope;

    @Builder.Default
    private List<String> focus = new ArrayList<>();

    @Builder.Default
    private List<String> requestedDimensions = new ArrayList<>();

    @Builder.Default
    private Filters filters = new Filters();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filters {
        private String inventor;
        private String college;
        private String patentType;
        private String legalStatus;
        private String assignee;
        private String keyword;
        private String technicalField;
        private String ipcMainClassInterpretation;
        private Integer applicationYearStart;
        private Integer applicationYearEnd;
        private Integer grantYearStart;
        private Integer grantYearEnd;
    }
}
