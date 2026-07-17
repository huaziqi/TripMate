package com.LHZ.TripMate.dto.admin;

import com.LHZ.TripMate.entity.KnowledgeDoc;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 知识库列表项：不含全文，只带预览，避免列表接口返回超大响应 */
@Data
@Builder
public class KnowledgeDocItemDTO {

    private Long id;
    private String spotKey;
    private String title;
    private KnowledgeDoc.Category category;
    private boolean enabled;
    private String sourceFileName;
    private int contentLength;
    private String preview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeDocItemDTO from(KnowledgeDoc doc) {
        String content = doc.getContent() == null ? "" : doc.getContent();
        return KnowledgeDocItemDTO.builder()
                .id(doc.getId())
                .spotKey(doc.getSpotKey())
                .title(doc.getTitle())
                .category(doc.getCategory())
                .enabled(doc.isEnabled())
                .sourceFileName(doc.getSourceFileName())
                .contentLength(content.length())
                .preview(content.length() > 120 ? content.substring(0, 120) + "…" : content)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
