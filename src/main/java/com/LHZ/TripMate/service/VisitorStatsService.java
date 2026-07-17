package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.VisitorImportResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface VisitorStatsService {

    /** 导入"景点景区旅游数据行为分析数据"xlsx：流式解析并按日/维度聚合入库（覆盖旧数据） */
    VisitorImportResultDTO importXlsx(MultipartFile file);
}
