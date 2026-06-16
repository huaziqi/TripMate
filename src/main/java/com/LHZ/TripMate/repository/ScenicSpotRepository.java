package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.ScenicSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenicSpotRepository
        extends JpaRepository<ScenicSpot, Long> {

    List<ScenicSpot> findByNameContainingIgnoreCase(String keyword);
}