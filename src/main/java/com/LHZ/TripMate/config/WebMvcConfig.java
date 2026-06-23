package com.LHZ.TripMate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // /uploads/** 由 FileServeController 处理，支持文件不存在时返回占位图
}
