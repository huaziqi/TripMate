package com.LHZ.TripMate.dto.route;

import java.util.List;

public class RecommendRouteDTO {

    @lombok.Getter
    @lombok.Setter
    private String id;
    @lombok.Getter
    @lombok.Setter
    private String name;
    @lombok.Getter
    @lombok.Setter
    private String theme;
    @lombok.Getter
    @lombok.Setter
    private String description;
    @lombok.Getter
    @lombok.Setter
    private String estimatedTime;
    @lombok.Getter
    @lombok.Setter
    private String guideText;
    @lombok.Getter
    @lombok.Setter
    private List<RouteSpotDTO> spots;

    public RecommendRouteDTO() {
    }

    public RecommendRouteDTO(String id, String name, String theme, String description,
                             String estimatedTime, String guideText, List<RouteSpotDTO> spots) {
        this.id = id;
        this.name = name;
        this.theme = theme;
        this.description = description;
        this.estimatedTime = estimatedTime;
        this.guideText = guideText;
        this.spots = spots;
    }

}