package com.LHZ.TripMate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绑定 application.yaml 中的 weather.api 配置段
 *
 * weather:
 *   api:
 *     path: https://qk5ctumxuw.re.qweatherapi.com
 *     key:  785df59a4de346d8afedd974a573dc8f
 */
@Data
@Component
@ConfigurationProperties(prefix = "weather.api")
public class WeatherConfig {

    /** 和风天气自定义域名 */
    private String path;

    /** 和风天气 API Key */
    private String key;
}
