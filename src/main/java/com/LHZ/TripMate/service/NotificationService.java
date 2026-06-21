package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.NotificationDTO;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.entity.Notification;

public interface NotificationService {
    void create(Notification.Type type, Long fromUserId, Long toUserId,
                Long postId, String postTitle, String commentContent);
    PageResult<NotificationDTO> list(Long userId, int page, int size);
    long unreadCount(Long userId);
    void markAllRead(Long userId);
}
