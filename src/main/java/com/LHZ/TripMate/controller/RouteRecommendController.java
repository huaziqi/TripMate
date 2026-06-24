package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.route.RecommendRouteDTO;
import com.LHZ.TripMate.dto.route.RouteSpotDTO;
import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.repository.ScenicSpotRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteRecommendController {

    private final ScenicSpotRepository scenicSpotRepository;

    public RouteRecommendController(ScenicSpotRepository scenicSpotRepository) {
        this.scenicSpotRepository = scenicSpotRepository;
    }

    @GetMapping("/recommend")
    public Result<List<RecommendRouteDTO>> recommendRoutes() {
        List<RecommendRouteDTO> routes = List.of(
                new RecommendRouteDTO(
                        "study-walk",
                        "入校学习漫步线",
                        "学习参观",
                        "从正门进入校园，途经主要教学楼最后到达中心图书馆，适合新生或游客快速熟悉校园环境。",
                        "约20分钟",
                        "入校学习漫步线从正门出发，依次经过八教、第二运动场和杏园，适合初次来到西南大学的游客了解校园学习与生活环境。",
                        List.of(
                                routeSpot("正门", "正门", "西南大学正门"),
                                routeSpot("8教", "8教", "八教", "八教学楼", "第八教学楼"),
                                routeSpot("中心图书馆", "图书馆", "西南大学中心图书馆", "中图")
                        )
                ),

                new RecommendRouteDTO(
                        "activity-checkin",
                        "校园活动打卡线",
                        "活动打卡",
                        "串联校园公共活动空间和标志性建筑，适合拍照打卡、参加活动前后游览。",
                        "约15分钟",
                        "校园活动打卡线从共青团花园出发，经过第四运动场，最后到达光大礼堂，适合拍照打卡和了解校园活动空间。",
                        List.of(
                                routeSpot("共青团花园", "共青团花园", "共青团广场"),
                                routeSpot("四运", "四运", "第四运动场", "四运动场"),
                                routeSpot("光大礼堂", "光大礼堂")
                        )
                ),

                new RecommendRouteDTO(
                        "life-service",
                        "生活服务体验线",
                        "生活服务",
                        "从校门进入，经过教学区后前往李园和李园食堂，适合了解学生日常学习和生活服务区域。",
                        "约15分钟",
                        "生活服务体验线从正门开始，经过八教，再前往李园和李园食堂，帮助游客了解校园学习与生活配套。",
                        List.of(
                                routeSpot("正门", "正门", "西南大学正门"),
                                routeSpot("8教", "8教", "八教", "八教学楼", "第八教学楼"),
                                routeSpot("李园", "李园"),
                                routeSpot("李园食堂", "李园食堂", "李园餐厅")
                        )
                ),

                new RecommendRouteDTO(
                        "humanity-teaching",
                        "教学楼参观线",
                        "学院教学",
                        "串联校门、新闻传媒学院和主要教学楼，适合展示校园教学空间与学院特色。",
                        "约45分钟",
                        "人文教学参观线从正门出发，途经新闻传媒学院、八教和二十五教，适合了解西南大学学院教学空间。",
                        List.of(
                                routeSpot("正门", "正门", "西南大学正门"),
                                routeSpot("新闻传媒学院", "新闻传媒学院", "新传"),
                                routeSpot("8教", "8教", "八教", "八教学楼", "第八教学楼"),
                                routeSpot("25教", "25教", "二十五教", "第二十五教学楼", "25教学楼")
                        )
                )
        );

        return Result.success(routes);
    }

    private RouteSpotDTO routeSpot(String displayName, String... keywords) {
        for (String keyword : keywords) {
            List<ScenicSpot> spots = scenicSpotRepository.findByNameContainingIgnoreCase(keyword);

            if (!spots.isEmpty()) {
                ScenicSpot spot = spots.get(0);

                return new RouteSpotDTO(
                        displayName,
                        spot.getId(),
                        spot.getName(),
                        spot.getAddress(),
                        spot.getLatitude(),
                        spot.getLongitude(),
                        true
                );
            }
        }

        return new RouteSpotDTO(
                displayName,
                null,
                displayName,
                "数据库中暂未匹配到该景点",
                null,
                null,
                false
        );
    }
}