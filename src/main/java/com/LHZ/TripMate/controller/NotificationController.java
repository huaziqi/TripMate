package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.NotificationDTO;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;

    @GetMapping
    public Result<PageResult<NotificationDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(notifService.list(userDetails.getWxUser().getId(), page, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(Map.of("count", notifService.unreadCount(userDetails.getWxUser().getId())));
    }

    @PostMapping("/read-all")
    public Result<Void> readAll(@AuthenticationPrincipal WxUserDetails userDetails) {
        notifService.markAllRead(userDetails.getWxUser().getId());
        return Result.success(null);
    }
}
