package com.LHZ.TripMate.dto;

import com.LHZ.TripMate.entity.Notification;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private Notification.Type type;
    private PostDTO.AuthorDTO fromUser;
    private Long postId;
    private String postTitle;
    private String commentContent;
    private boolean read;
    private LocalDateTime createdAt;
}
