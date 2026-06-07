package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.badge.BadgeDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public Result<List<BadgeDTO>> listBadges(
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(badgeService.listAllBadges(userDetails.getUsername()));
    }

    @PostMapping("/{id}/unlock")
    public Result<BadgeDTO> unlock(
            @PathVariable Long id,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(badgeService.unlockBadge(userDetails.getUsername(), id));
    }
}
