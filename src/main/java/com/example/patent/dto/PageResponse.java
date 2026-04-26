package com.example.patent.dto;

import com.example.patent.entity.PatentInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "当前页")
    private Integer page;

    @Schema(description = "每页数量")
    private Integer size;

    public static <T> PageResponse<T> of(List<T> list, Long total, Integer page, Integer size) {
        PageResponse<T> response = new PageResponse<>();
        response.setList(list);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }
}
