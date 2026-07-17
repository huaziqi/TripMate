package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 景点结构化知识条目：每个景点ID（如 LS-001）一条记录，
 * 各字段独立维护，对应"景点结构化数据集"的表格列。
 */
@Data
@Entity
@Table(name = "knowledge_spot_entry",
       uniqueConstraints = @UniqueConstraint(columnNames = {"spot_key", "spot_code"}))
public class KnowledgeSpotEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 数字人景区 key，与 GuideSpotConfig.spotKey 对应，如 lingshan */
    @Column(name = "spot_key", nullable = false, length = 50)
    private String spotKey;

    /** 景点ID，如 LS-001 / NH-003 */
    @Column(name = "spot_code", nullable = false, length = 50)
    private String spotCode;

    /** 景区名称（子表维度），如 灵山胜境 / 拈花湾禅意小镇 */
    @Column(name = "zone_name", length = 100)
    private String zoneName;

    /** 景点名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 具体位置 */
    @Column(columnDefinition = "TEXT")
    private String location;

    /** 建筑/景观参数 */
    @Column(name = "scale_info", columnDefinition = "TEXT")
    private String scaleInfo;

    /** 核心功能 */
    @Column(name = "core_function", columnDefinition = "TEXT")
    private String coreFunction;

    /** 文化内涵 */
    @Column(columnDefinition = "TEXT")
    private String culture;

    /** 详细介绍 */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    /** 游玩亮点 */
    @Column(name = "tour_tips", columnDefinition = "TEXT")
    private String tourTips;

    /** 演艺/开放信息 */
    @Column(name = "ticket_info", columnDefinition = "TEXT")
    private String ticketInfo;

    /** 备注 */
    @Column(columnDefinition = "TEXT")
    private String remark;

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
