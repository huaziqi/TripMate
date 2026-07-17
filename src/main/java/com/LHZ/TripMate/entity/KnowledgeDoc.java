package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 景区知识文档：讲解词 / 文史资料 / 常见问题等，作为数字人的知识基础。
 * spotKey 为空表示通用知识（对所有景点数字人生效）。
 */
@Data
@Entity
@Table(name = "knowledge_doc")
public class KnowledgeDoc {

    public enum Category {
        EXPLANATION,  // 讲解词
        HISTORY,      // 文史资料
        FAQ,          // 常见问题及答案
        OTHER         // 其他
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_key", length = 50)
    private String spotKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category = Category.OTHER;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
