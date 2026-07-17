package com.LHZ.TripMate.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 游客行为数据导入结果 */
@Data
@AllArgsConstructor
public class VisitorImportResultDTO {
    private long totalRows;
    private long skippedRows;
    private String dateFrom;
    private String dateTo;
}
