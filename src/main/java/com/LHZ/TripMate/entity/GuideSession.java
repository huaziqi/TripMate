package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guide_session",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "spot_key"}))
public class GuideSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_key", nullable = false, length = 50)
    private String spotKey;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }
}
