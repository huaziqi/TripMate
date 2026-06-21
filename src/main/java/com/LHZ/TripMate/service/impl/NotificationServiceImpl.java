package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.NotificationDTO;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.dto.PostDTO;
import com.LHZ.TripMate.entity.Notification;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.NotificationRepository;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifRepo;
    private final WxUserRepository wxUserRepo;

    @Override
    public void create(Notification.Type type, Long fromUserId, Long toUserId,
                       Long postId, String postTitle, String commentContent) {
        if (fromUserId != null && fromUserId.equals(toUserId)) return;
        notifRepo.save(Notification.builder()
                .type(type)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .postId(postId)
                .postTitle(postTitle)
                .commentContent(commentContent)
                .build());
    }

    @Override
    public PageResult<NotificationDTO> list(Long userId, int page, int size) {
        Page<Notification> pg = notifRepo.findByToUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        var items = pg.getContent().stream().map(this::toDTO).toList();
        return new PageResult<>(items, pg.getTotalElements(), page, size);
    }

    @Override
    public long unreadCount(Long userId) {
        return notifRepo.countByToUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notifRepo.markAllReadByToUserId(userId);
    }

    private NotificationDTO toDTO(Notification n) {
        PostDTO.AuthorDTO fromUser = null;
        if (n.getFromUserId() != null) {
            WxUser u = wxUserRepo.findById(n.getFromUserId()).orElse(null);
            if (u != null) {
                fromUser = PostDTO.AuthorDTO.builder()
                        .id(u.getId())
                        .nickname(u.getNickname())
                        .avatarUrl(u.getAvatarUrl())
                        .build();
            }
        }
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .fromUser(fromUser)
                .postId(n.getPostId())
                .postTitle(n.getPostTitle())
                .commentContent(n.getCommentContent())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
