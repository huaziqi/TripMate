package com.LHZ.TripMate.dto.route;

import lombok.Data;

import java.util.List;

/** 个性化推荐结果中的一条路线 */
@Data
public class PersonalizedRouteDTO {

    private String id;
    private String name;
    private String theme;
    private String description;
    private String estimatedTime;
    private String guideText;

    /** 匹配度 0-100 */
    private int matchScore;

    /** 可解释的推荐理由，如 “你选择了「历史文化」兴趣” */
    private List<String> matchReasons;

    /** 路线特色标签展示名，如 [历史文化, 佛教艺术] */
    private List<String> tags;

    /** 适合人群一句话，如 “适合体力充沛的深度游客” */
    private String suitableFor;

    private List<PersonalizedRouteSpotDTO> spots;
}
