package com.LHZ.TripMate.service;

import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.repository.ScenicSpotRepository;
import org.springframework.stereotype.Service;
import com.LHZ.TripMate.dto.NearbySpotDTO;
import java.util.Comparator;

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

    @Override
    public List<NearbySpotDTO> findNearby(
            double latitude,
            double longitude,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        return scenicSpotRepository.findAll()
                .stream()
                .filter(spot ->
                        spot.getLatitude() != null &&
                                spot.getLongitude() != null
                )
                .map(spot -> {
                    double distance = calculateDistance(
                            latitude,
                            longitude,
                            spot.getLatitude(),
                            spot.getLongitude()
                    );

                    return new NearbySpotDTO(
                            spot.getId(),
                            spot.getName(),
                            spot.getAddress(),
                            spot.getCategory(),
                            spot.getLatitude(),
                            spot.getLongitude(),
                            distance
                    );
                })
                .sorted(Comparator.comparingDouble(
                        NearbySpotDTO::getDistance
                ))
                .limit(safeLimit)
                .toList();
    }
    private double calculateDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        final double earthRadius = 6371000.0;

        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);

        double latitudeDifference =
                Math.toRadians(latitude2 - latitude1);

        double longitudeDifference =
                Math.toRadians(longitude2 - longitude1);

        double a =
                Math.sin(latitudeDifference / 2) *
                        Math.sin(latitudeDifference / 2) +
                        Math.cos(lat1) *
                                Math.cos(lat2) *
                                Math.sin(longitudeDifference / 2) *
                                Math.sin(longitudeDifference / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return earthRadius * c;
    }
}