package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "spot_favorite",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "spot_id"})
        }
)
public class SpotFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_id", nullable = false)
    private Long spotId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public SpotFavorite() {
    }

    public SpotFavorite(Long userId, Long spotId) {
        this.userId = userId;
        this.spotId = spotId;
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

    public Long getSpotId() {
        return spotId;
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

    public void setSpotId(Long spotId) {
        this.spotId = spotId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}