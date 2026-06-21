package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "post_comment")
public class PostComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 500) private String content;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "parent_id") private Long parentId;   // null = 顶层评论
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
}
