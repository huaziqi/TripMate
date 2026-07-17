package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.SaveKnowledgeSpotEntryDTO;
import com.LHZ.TripMate.dto.admin.SpotImportResultDTO;
import com.LHZ.TripMate.entity.KnowledgeSpotEntry;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeSpotService {

    List<KnowledgeSpotEntry> list(String spotKey, String zoneName, String keyword);

    List<String> zoneNames();

    KnowledgeSpotEntry get(Long id);

    KnowledgeSpotEntry create(SaveKnowledgeSpotEntryDTO dto);

    KnowledgeSpotEntry update(Long id, SaveKnowledgeSpotEntryDTO dto);

    void delete(Long id);

    /** 导入结构化数据集 docx：解析表格行，按（spotKey + 景点ID）新增或更新 */
    SpotImportResultDTO importDocx(MultipartFile file, String spotKey);
}
