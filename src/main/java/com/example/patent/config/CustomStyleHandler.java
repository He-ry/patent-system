package com.example.patent.config;

import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.handler.CellWriteHandler;
import org.apache.poi.ss.usermodel.*;

public class CustomStyleHandler implements CellWriteHandler {

    private CellStyle headerStyle;
    private CellStyle dataStyle;
    private boolean stylesInitialized = false;

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        if (!stylesInitialized) {
            Workbook workbook = context.getWriteSheetHolder().getSheet().getWorkbook();
            headerStyle = createHeaderStyle(workbook);
            dataStyle = createDataStyle(workbook);
            stylesInitialized = true;
        }

        Cell cell = context.getCell();
        int rowIndex = cell.getRowIndex();

        if (rowIndex == 0) {
            cell.setCellStyle(headerStyle);
            Row row = cell.getRow();
            row.setHeightInPoints(85);
        } else {
            cell.setCellStyle(dataStyle);
            Row row = cell.getRow();
            row.setHeightInPoints(-1);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setFontName("SimHei");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);

        Font font = workbook.createFont();
        font.setFontName("SimSun");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }
}
