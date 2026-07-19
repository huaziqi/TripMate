package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.route.InterestOptionDTO;
import com.LHZ.TripMate.dto.route.PersonalizeRequestDTO;
import com.LHZ.TripMate.dto.route.PersonalizedRecommendationDTO;
import com.LHZ.TripMate.dto.route.RecommendRouteDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.RouteRecommendService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteRecommendController {

    private final RouteRecommendService routeRecommendService;

    public RouteRecommendController(RouteRecommendService routeRecommendService) {
        this.routeRecommendService = routeRecommendService;
    }

    /** 通用推荐（无画像），保持旧接口兼容 */
    @GetMapping("/recommend")
    public Result<List<RecommendRouteDTO>> recommendRoutes() {
        return Result.success(routeRecommendService.defaultRoutes());
    }

    /** 兴趣问卷选项 */
    @GetMapping("/recommend/options")
    public Result<List<InterestOptionDTO>> interestOptions() {
        return Result.success(routeRecommendService.interestOptions());
    }

    /**
     * 个性化推荐。登录用户（携带 token）会叠加收藏 / 浏览 / 数字人对话行为信号；
     * 未登录仅用问卷画像。
     */
    @PostMapping("/recommend/personalized")
    public Result<PersonalizedRecommendationDTO> personalized(
            @RequestBody(required = false) PersonalizeRequestDTO request,
            @AuthenticationPrincipal WxUserDetails userDetails) {

        Long userId = userDetails != null && userDetails.getWxUser() != null
                ? userDetails.getWxUser().getId()
                : null;

        return Result.success(routeRecommendService.personalize(request, userId));
    }
}
