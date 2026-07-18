package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.GuideSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GuideSessionRepository extends JpaRepository<GuideSession, Long> {
    Optional<GuideSession> findByUserIdAndSpotKey(Long userId, String spotKey);

    List<GuideSession> findByUserId(Long userId);
}
