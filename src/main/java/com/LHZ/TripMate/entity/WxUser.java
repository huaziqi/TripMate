package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wx_user")
public class WxUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String openid;

    @Column(length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
