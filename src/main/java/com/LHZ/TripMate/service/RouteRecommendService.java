package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.route.InterestOptionDTO;
import com.LHZ.TripMate.dto.route.PersonalizeRequestDTO;
import com.LHZ.TripMate.dto.route.PersonalizedRecommendationDTO;
import com.LHZ.TripMate.dto.route.RecommendRouteDTO;

import java.util.List;

public interface RouteRecommendService {

    /** 通用推荐（不带画像），保持旧接口兼容 */
    List<RecommendRouteDTO> defaultRoutes();

    /** 兴趣问卷选项，供前端渲染 */
    List<InterestOptionDTO> interestOptions();

    /**
     * 个性化推荐：问卷画像 + 登录用户行为信号（收藏/浏览/对话）合成兴趣向量，
     * 对路线打分排序，并为每个景点挑选贴合兴趣的讲解重点。
     *
     * @param userId 登录用户 id，可为 null（仅用问卷画像）
     */
    PersonalizedRecommendationDTO personalize(PersonalizeRequestDTO request, Long userId);
}
