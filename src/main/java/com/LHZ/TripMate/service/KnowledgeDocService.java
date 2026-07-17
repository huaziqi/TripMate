package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.KnowledgeDocItemDTO;
import com.LHZ.TripMate.dto.admin.SaveKnowledgeDocDTO;
import com.LHZ.TripMate.entity.KnowledgeDoc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocService {

    List<KnowledgeDocItemDTO> list(String spotKey, KnowledgeDoc.Category category, String keyword);

    KnowledgeDoc get(Long id);

    KnowledgeDoc create(SaveKnowledgeDocDTO dto);

    KnowledgeDoc update(Long id, SaveKnowledgeDocDTO dto);

    void delete(Long id);

    /** 上传知识文档文件（.docx / .txt / .md），解析正文入库 */
    KnowledgeDoc importFile(MultipartFile file, String spotKey,
                            KnowledgeDoc.Category category, String title);
}
