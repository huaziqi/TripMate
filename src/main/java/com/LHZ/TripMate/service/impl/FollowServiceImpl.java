package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.FollowStatsDTO;
import com.LHZ.TripMate.entity.Notification;
import com.LHZ.TripMate.entity.UserFollow;
import com.LHZ.TripMate.repository.UserFollowRepository;
import com.LHZ.TripMate.service.FollowService;
import com.LHZ.TripMate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowRepository followRepo;
    private final NotificationService notifService;

    @Override
    @Transactional
    public Map<String, Object> toggleFollow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        Optional<UserFollow> existing = followRepo.findByFollowerIdAndFollowingId(currentUserId, targetUserId);
        boolean following;
        if (existing.isPresent()) {
            followRepo.delete(existing.get());
            following = false;
        } else {
            followRepo.save(UserFollow.builder()
                    .followerId(currentUserId).followingId(targetUserId).build());
            following = true;
            notifService.create(Notification.Type.NEW_FOLLOWER, currentUserId, targetUserId,
                    null, null, null);
        }
        long followerCount = followRepo.countByFollowingId(targetUserId);
        return Map.of("following", following, "followerCount", followerCount);
    }

    @Override
    public FollowStatsDTO getStats(Long targetUserId, Long currentUserId) {
        long followerCount = followRepo.countByFollowingId(targetUserId);
        long followingCount = followRepo.countByFollowerId(targetUserId);
        boolean isFollowing = currentUserId != null &&
                followRepo.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        return new FollowStatsDTO(followerCount, followingCount, isFollowing);
    }
}
