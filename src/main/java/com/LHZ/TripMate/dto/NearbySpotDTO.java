package com.LHZ.TripMate.dto;

public class NearbySpotDTO {

    @lombok.Getter
    @lombok.Setter
    private Long id;
    @lombok.Setter
    @lombok.Getter
    private String name;
    @lombok.Setter
    @lombok.Getter
    private String address;
    @lombok.Setter
    @lombok.Getter
    private String category;
    @lombok.Setter
    @lombok.Getter
    private Double latitude;
    @lombok.Setter
    @lombok.Getter
    private Double longitude;

    // 与当前位置的距离，单位：米
    @lombok.Setter
    @lombok.Getter
    private Double distance;

    public NearbySpotDTO() {
    }

    public NearbySpotDTO(
            Long id,
            String name,
            String address,
            String category,
            Double latitude,
            Double longitude,
            Double distance
    ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
    }

}