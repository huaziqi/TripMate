package com.LHZ.TripMate.dto.route;

public class RouteSpotDTO {

    private String displayName;
    private Long spotId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private boolean matched;

    public RouteSpotDTO() {
    }

    public RouteSpotDTO(String displayName, Long spotId, String name, String address,
                        Double latitude, Double longitude, boolean matched) {
        this.displayName = displayName;
        this.spotId = spotId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.matched = matched;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getSpotId() {
        return spotId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setSpotId(Long spotId) {
        this.spotId = spotId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }
}