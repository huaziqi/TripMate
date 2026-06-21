package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.FollowStatsDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public Result<?> toggleFollow(@PathVariable Long userId,
                                  @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(followService.toggleFollow(userDetails.getWxUser().getId(), userId));
    }

    @GetMapping("/{userId}/stats")
    public Result<FollowStatsDTO> getStats(@PathVariable Long userId,
                                           @AuthenticationPrincipal(errorOnInvalidType = false)
                                           WxUserDetails userDetails) {
        Long currentId = userDetails != null ? userDetails.getWxUser().getId() : null;
        return Result.success(followService.getStats(userId, currentId));
    }
}
