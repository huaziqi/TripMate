package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.entity.SpotFavorite;
import com.LHZ.TripMate.repository.ScenicSpotRepository;
import com.LHZ.TripMate.repository.SpotFavoriteRepository;
import com.LHZ.TripMate.service.SpotFavoriteService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SpotFavoriteServiceImpl implements SpotFavoriteService {

    private final SpotFavoriteRepository spotFavoriteRepository;
    private final ScenicSpotRepository scenicSpotRepository;

    public SpotFavoriteServiceImpl(
            SpotFavoriteRepository spotFavoriteRepository,
            ScenicSpotRepository scenicSpotRepository
    ) {
        this.spotFavoriteRepository = spotFavoriteRepository;
        this.scenicSpotRepository = scenicSpotRepository;
    }

    @Override
    public void addFavorite(Long userId, Long spotId) {
        if (spotFavoriteRepository.existsByUserIdAndSpotId(userId, spotId)) {
            return;
        }

        spotFavoriteRepository.save(new SpotFavorite(userId, spotId));
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long spotId) {
        spotFavoriteRepository.deleteByUserIdAndSpotId(userId, spotId);
    }

    @Override
    public boolean isFavorited(Long userId, Long spotId) {
        return spotFavoriteRepository.existsByUserIdAndSpotId(userId, spotId);
    }

    @Override
    public List<ScenicSpot> listFavoriteSpots(Long userId) {
        return spotFavoriteRepository.findByUserIdOrderByCreateTimeDesc(userId)
                .stream()
                .map(favorite -> scenicSpotRepository.findById(favorite.getSpotId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }
}