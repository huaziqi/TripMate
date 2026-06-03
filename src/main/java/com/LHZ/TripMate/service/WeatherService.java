package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.WeatherDTO;

public interface WeatherService {

    /**
     * 根据 GPS 坐标查询实时天气
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 天气数据
     */
    WeatherDTO getWeather(double longitude, double latitude);
}
