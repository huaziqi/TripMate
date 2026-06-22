package com.LHZ.TripMate.service;

import com.LHZ.TripMate.entity.ScenicSpot;

import java.util.List;

public interface SpotFavoriteService {

    void addFavorite(Long userId, Long spotId);

    void removeFavorite(Long userId, Long spotId);

    boolean isFavorited(Long userId, Long spotId);

    List<ScenicSpot> listFavoriteSpots(Long userId);
}