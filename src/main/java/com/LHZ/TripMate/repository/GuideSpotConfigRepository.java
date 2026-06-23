package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.GuideSpotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GuideSpotConfigRepository extends JpaRepository<GuideSpotConfig, Long> {
    Optional<GuideSpotConfig> findBySpotKey(String spotKey);
    Optional<GuideSpotConfig> findBySpotKeyAndActiveTrue(String spotKey);
}
