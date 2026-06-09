package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.badge.BadgeDTO;
import java.util.List;

public interface BadgeService {
    List<BadgeDTO> listAllBadges(String openid);
    BadgeDTO unlockBadge(String openid, Long badgeId);
}
