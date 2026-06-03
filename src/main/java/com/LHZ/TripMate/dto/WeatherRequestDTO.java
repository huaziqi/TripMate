package com.LHZ.TripMate.dto;

import lombok.Data;

/**
 * 前端上报的坐标，对应 useApi.ts 中 post('/api/weather', { longitude, latitude })
 */
@Data
public class WeatherRequestDTO {

    /** 经度 */
    private double longitude;

    /** 纬度 */
    private double latitude;
}
