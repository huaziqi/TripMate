package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.SpotFavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class SpotFavoriteController {

    private final SpotFavoriteService spotFavoriteService;

    public SpotFavoriteController(SpotFavoriteService spotFavoriteService) {
        this.spotFavoriteService = spotFavoriteService;
    }

    @PostMapping("/{spotId}")
    public Result<Void> addFavorite(
            @PathVariable Long spotId,
            @AuthenticationPrincipal WxUserDetails userDetails
    ) {
        Long userId = getCurrentUserId(userDetails);
        spotFavoriteService.addFavorite(userId, spotId);
        return Result.success();
    }

    @PostMapping("/{spotId}/delete")
    public Result<Void> removeFavoriteByPost(
            @PathVariable Long spotId,
            @AuthenticationPrincipal WxUserDetails userDetails
    ) {
        Long userId = getCurrentUserId(userDetails);
        spotFavoriteService.removeFavorite(userId, spotId);
        return Result.success();
    }

    @DeleteMapping("/{spotId}")
    public Result<Void> removeFavorite(
            @PathVariable Long spotId,
            @AuthenticationPrincipal WxUserDetails userDetails
    ) {
        Long userId = getCurrentUserId(userDetails);
        spotFavoriteService.removeFavorite(userId, spotId);
        return Result.success();
    }

    @GetMapping("/check/{spotId}")
    public Result<Boolean> checkFavorite(
            @PathVariable Long spotId,
            @AuthenticationPrincipal WxUserDetails userDetails
    ) {
        Long userId = getCurrentUserId(userDetails);
        return Result.success(spotFavoriteService.isFavorited(userId, spotId));
    }

    @GetMapping
    public Result<List<ScenicSpot>> listFavorites(
            @AuthenticationPrincipal WxUserDetails userDetails
    ) {
        Long userId = getCurrentUserId(userDetails);
        return Result.success(spotFavoriteService.listFavoriteSpots(userId));
    }

    private Long getCurrentUserId(WxUserDetails userDetails) {
        if (userDetails == null || userDetails.getWxUser() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        return userDetails.getWxUser().getId();
    }
}