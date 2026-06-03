package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 返回给前端的天气数据，字段名与前端 WeatherData 接口保持一致
 *
 * <pre>
 * interface WeatherData {
 *   city, weather, temperature, winddirection, windpower, humidity, reporttime
 * }
 * </pre>
 */
@Data
@Builder
public class WeatherDTO {

    /** 城市名，如 "北京" */
    private String city;

    /** 天气状况，如 "晴"、"多云" */
    private String weather;

    /** 温度（摄氏度），如 "25" */
    private String temperature;

    /** 风向，如 "东北风" */
    private String winddirection;

    /** 风力等级，如 "3-4" */
    private String windpower;

    /** 相对湿度（%），如 "65" */
    private String humidity;

    /** 数据观测时间，格式 "MM-dd HH:mm" */
    private String reporttime;
}
