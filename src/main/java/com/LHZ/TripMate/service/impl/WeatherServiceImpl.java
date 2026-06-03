package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.config.WeatherConfig;
import com.LHZ.TripMate.dto.WeatherDTO;
import com.LHZ.TripMate.service.WeatherService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    private final WeatherConfig config;
    private final RestClient restClient;

    public WeatherServiceImpl(WeatherConfig config) {
        this.config = config;
        this.restClient = RestClient.builder()
                .baseUrl(config.getPath())
                .build();
    }

    // ----------------------------------------------------------------
    // 公开方法
    // ----------------------------------------------------------------

    @Override
    public WeatherDTO getWeather(double longitude, double latitude) {
        // 和风天气 location 参数格式：经度,纬度
        String location = String.format("%.2f,%.2f", longitude, latitude);

        String cityName = fetchCityName(location);
        QWeatherNowResponse nowRes = fetchNowWeather(location);

        if (!"200".equals(nowRes.getCode())) {
            throw new RuntimeException("和风天气接口异常，code=" + nowRes.getCode());
        }

        QWeatherNow now = nowRes.getNow();

        return WeatherDTO.builder()
                .city(cityName)
                .weather(now.getText())
                .temperature(now.getTemp())
                .winddirection(now.getWindDir())
                .windpower(now.getWindScale())
                .humidity(now.getHumidity())
                .reporttime(formatObsTime(now.getObsTime()))
                .build();
    }

    // ----------------------------------------------------------------
    // 私有方法
    // ----------------------------------------------------------------

    /** 逆地理：坐标 → 城市名 */
    private String fetchCityName(String location) {
        try {
            QGeoCityLookupResponse geoRes = restClient.get()
                    .uri(b -> b.path("/geo/v2/city/lookup")
                               .queryParam("location", location)
                               .queryParam("key", config.getKey())
                               .build())
                    .retrieve()
                    .body(QGeoCityLookupResponse.class);

            if (geoRes != null
                    && "200".equals(geoRes.getCode())
                    && geoRes.getLocation() != null
                    && !geoRes.getLocation().isEmpty()) {
                return geoRes.getLocation().get(0).getAdm2();
            }
        } catch (Exception e) {
            log.warn("城市名查询失败，location={}，原因：{}", location, e.getMessage());
        }
        return "";
    }

    /** 实时天气：坐标 → 天气数据 */
    private QWeatherNowResponse fetchNowWeather(String location) {
        return restClient.get()
                .uri(b -> b.path("/v7/weather/now")
                           .queryParam("location", location)
                           .queryParam("key", config.getKey())
                           .build())
                .retrieve()
                .body(QWeatherNowResponse.class);
    }

    /**
     * 将 ISO-8601 时间（"2026-05-26T14:00+08:00"）格式化为 "05-26 14:00"
     */
    private String formatObsTime(String obsTime) {
        if (obsTime == null || obsTime.isBlank()) return "";
        try {
            return OffsetDateTime.parse(obsTime)
                    .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        } catch (Exception e) {
            return obsTime;
        }
    }

    // ================================================================
    // 和风天气 API 响应模型（仅供 Jackson 反序列化，不对外暴露）
    // ================================================================

    @Data
    private static class QWeatherNowResponse {
        private String code;
        private String updateTime;
        private QWeatherNow now;
    }

    @Data
    private static class QWeatherNow {
        /** 数据观测时间 */
        private String obsTime;
        /** 温度（℃） */
        private String temp;
        /** 体感温度 */
        private String feelsLike;
        /** 天气状况文字 */
        private String text;
        /** 风向 */
        private String windDir;
        /** 风力等级 */
        private String windScale;
        /** 风速（km/h） */
        private String windSpeed;
        /** 相对湿度（%） */
        private String humidity;
    }

    @Data
    private static class QGeoCityLookupResponse {
        private String code;
        private List<QGeoLocation> location;
    }

    @Data
    private static class QGeoLocation {
        /** 地区/城市名称 */
        private String name;
        /** 地区/城市 ID */
        private String id;
        /** 上级行政区划，如 "北京" */
        private String adm2;
        /** 所属一级行政区，如 "北京市" */
        private String adm1;
    }
}
