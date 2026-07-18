package com.LHZ.TripMate.dto.route;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 个性化推荐整体响应：画像解读 + 按匹配度排序的路线 */
@Data
public class PersonalizedRecommendationDTO {

    /** 画像来源解读，逐条展示，如 “问卷选择：历史文化、祈福体验” */
    private List<String> profileSummary;

    /** 兴趣维度展示名 -> 百分比权重（用于前端画像条形图） */
    private Map<String, Integer> interestWeights;

    /** 按匹配度从高到低排序的路线 */
    private List<PersonalizedRouteDTO> routes;
}
