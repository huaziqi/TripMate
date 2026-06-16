package com.LHZ.TripMate.service;

import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.repository.ScenicSpotRepository;
import com.LHZ.TripMate.service.ScenicSpotService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScenicSpotServiceImpl implements ScenicSpotService {

    private final ScenicSpotRepository scenicSpotRepository;

    public ScenicSpotServiceImpl(
            ScenicSpotRepository scenicSpotRepository
    ) {
        this.scenicSpotRepository = scenicSpotRepository;
    }

    @Override
    public List<ScenicSpot> findAll() {
        return scenicSpotRepository.findAll();
    }

    @Override
    public Optional<ScenicSpot> findById(Long id) {
        return scenicSpotRepository.findById(id);
    }

    @Override
    public List<ScenicSpot> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return scenicSpotRepository.findAll();
        }

        return scenicSpotRepository
                .findByNameContainingIgnoreCase(keyword.trim());
    }

    @Override
    public ScenicSpot save(ScenicSpot scenicSpot) {
        return scenicSpotRepository.save(scenicSpot);
    }
}