package com.LHZ.TripMate.dto.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 兴趣问卷选项，由后端下发，前端渲染问卷 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestOptionDTO {

    /** 维度 key，如 history */
    private String key;

    /** 展示名，如 历史文化 */
    private String label;

    /** 一句话说明，帮助游客选择 */
    private String description;

    /** 展示用 emoji 图标 */
    private String icon;
}
