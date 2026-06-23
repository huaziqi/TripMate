package com.LHZ.TripMate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekConfig {
    private String url;
    private String key;
    private String model;
}
