package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "history_record")
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String type;

    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String content;

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
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}