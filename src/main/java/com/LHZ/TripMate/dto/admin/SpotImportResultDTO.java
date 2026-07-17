package com.LHZ.TripMate.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

/** docx 结构化数据集导入结果 */
@Data
@AllArgsConstructor
public class SpotImportResultDTO {
    private int created;
    private int updated;
    private int skipped;
}
