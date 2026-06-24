package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "history_record")
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * VIEW_SPOT / FAVORITE_SPOT / SEARCH_SPOT / PLAY_AUDIO / AI_CHAT
     */
    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public HistoryRecord() {
    }

    public HistoryRecord(Long userId, String type, Long targetId, String content) {
        this.userId = userId;
        this.type = type;
        this.targetId = targetId;
        this.content = content;
    }

    @PrePersist
    void prePersist() {
        createTime = LocalDateTime.now();
    }
}