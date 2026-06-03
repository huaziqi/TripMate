package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.common.ResultCode;
import com.LHZ.TripMate.dto.WeatherDTO;
import com.LHZ.TripMate.dto.WeatherRequestDTO;
import com.LHZ.TripMate.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 获取实时天气
     * <p>
     * POST /api/weather
     * Body: { "longitude": 116.39, "latitude": 39.91 }
     */
    @PostMapping("/weather")
    public Result<WeatherDTO> getWeather(@RequestBody WeatherRequestDTO request) {
        try {
            WeatherDTO data = weatherService.getWeather(
                    request.getLongitude(),
                    request.getLatitude()
            );
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取天气失败，longitude={}, latitude={}，原因：{}",
                    request.getLongitude(), request.getLatitude(), e.getMessage());
            return Result.fail(ResultCode.SERVER_ERROR);
        }
    }
}
