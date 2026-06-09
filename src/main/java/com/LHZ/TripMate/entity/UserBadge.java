package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_badge",
       uniqueConstraints = @UniqueConstraint(columnNames = {"openid", "badge_id"}))
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String openid;

    @Column(name = "badge_id", nullable = false)
    private Long badgeId;

    @Column(name = "unlocked_at", updatable = false)
    private LocalDateTime unlockedAt;

    @Column(length = 200)
    private String note;

    @PrePersist
    void prePersist() {
        unlockedAt = LocalDateTime.now();
    }
}
