package com.LHZ.TripMate.service;

import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.dto.NearbySpotDTO;

import java.util.List;
import java.util.Optional;

public interface ScenicSpotService {

    // 查询全部景点
    List<ScenicSpot> findAll();

    // 根据 ID 查询景点
    Optional<ScenicSpot> findById(Long id);

    // 根据景点名称模糊搜索
    List<ScenicSpot> searchByName(String keyword);

    // 保存景点，后面管理员新增景点时也能用
    ScenicSpot save(ScenicSpot scenicSpot);

    List<NearbySpotDTO> findNearby(
            double latitude,
            double longitude,
            int limit
    );
}