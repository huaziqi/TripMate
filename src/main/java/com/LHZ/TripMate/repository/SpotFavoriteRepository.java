package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.SpotFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpotFavoriteRepository extends JpaRepository<SpotFavorite, Long> {

    boolean existsByUserIdAndSpotId(Long userId, Long spotId);

    Optional<SpotFavorite> findByUserIdAndSpotId(Long userId, Long spotId);

    List<SpotFavorite> findByUserIdOrderByCreateTimeDesc(Long userId);

    void deleteByUserIdAndSpotId(Long userId, Long spotId);
}