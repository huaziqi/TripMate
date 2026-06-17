package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.service.ScenicSpotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.LHZ.TripMate.dto.NearbySpotDTO;

import java.util.List;

@RestController
@RequestMapping("/api/spots")
public class ScenicSpotController {

    private final ScenicSpotService scenicSpotService;

    public ScenicSpotController(ScenicSpotService scenicSpotService) {
        this.scenicSpotService = scenicSpotService;
    }

    /**
     * 查询全部景点
     * GET /api/spots
     */
    @GetMapping
    public List<ScenicSpot> findAll() {
        return scenicSpotService.findAll();
    }

    /**
     * 根据景点名称模糊搜索
     * GET /api/spots/search?keyword=博物馆
     */
    @GetMapping("/search")
    public List<ScenicSpot> search(
            @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        return scenicSpotService.searchByName(keyword);
    }

    @GetMapping("/nearby")
    public List<NearbySpotDTO> findNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") int limit
    ){
        return scenicSpotService.findNearby(
                latitude,
                longitude,
                limit
        );
    };

    /**
     * 根据 ID 查询景点详情
     * GET /api/spots/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScenicSpot> findById(@PathVariable Long id) {
        return scenicSpotService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}