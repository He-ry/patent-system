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
public class ReportIntent {
    private String rawQuery;
    private String titleHint;
    private String scope;
    private List<String> focus;
    private List<String> requestedDimensions;
    private Filters filters;

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

    public List<String> getFocus() {
        return focus == null ? new ArrayList<>() : focus;
    }

    public List<String> getRequestedDimensions() {
        return requestedDimensions == null ? new ArrayList<>() : requestedDimensions;
    }

    public Filters getFilters() {
        return filters == null ? new Filters() : filters;
    }
}
