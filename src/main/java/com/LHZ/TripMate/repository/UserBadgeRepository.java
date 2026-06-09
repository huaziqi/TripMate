package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByOpenidOrderByUnlockedAtDesc(String openid);
    Optional<UserBadge> findByOpenidAndBadgeId(String openid, Long badgeId);
    boolean existsByOpenidAndBadgeId(String openid, Long badgeId);
}
