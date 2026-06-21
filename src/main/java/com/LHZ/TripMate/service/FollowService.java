package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.FollowStatsDTO;
import java.util.Map;

public interface FollowService {
    Map<String, Object> toggleFollow(Long currentUserId, Long targetUserId);
    FollowStatsDTO getStats(Long targetUserId, Long currentUserId);
}
