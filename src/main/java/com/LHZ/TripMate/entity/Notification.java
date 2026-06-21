package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification",
    indexes = @Index(columnList = "to_user_id,is_read,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    public enum Type { LIKE_POST, COMMENT_POST, NEW_FOLLOWER, MENTION_COMMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "post_title")
    private String postTitle;

    @Column(name = "comment_content", length = 200)
    private String commentContent;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
