package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.HistoryAddRequestDTO;
import com.LHZ.TripMate.entity.HistoryRecord;
import com.LHZ.TripMate.repository.HistoryRecordRepository;
import com.LHZ.TripMate.security.WxUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryRecordController {

    private final HistoryRecordRepository historyRecordRepository;

    public HistoryRecordController(HistoryRecordRepository historyRecordRepository) {
        this.historyRecordRepository = historyRecordRepository;
    }

    @PostMapping
    public Result<HistoryRecord> addHistory(
            @RequestBody HistoryAddRequestDTO req,
            @AuthenticationPrincipal WxUserDetails userDetails
    ) {
        Long userId = getCurrentUserId(userDetails);

        HistoryRecord record = new HistoryRecord(
                userId,
                req.getType(),
                req.getTargetId(),
                req.getContent()
        );

        return Result.success(historyRecordRepository.save(record));
    }

    @GetMapping
    public Result<List<HistoryRecord>> listHistory(
            @AuthenticationPrincipal WxUserDetails userDetails
    ) {
        Long userId = getCurrentUserId(userDetails);

        return Result.success(
                historyRecordRepository.findTop50ByUserIdOrderByCreateTimeDesc(userId)
        );
    }

    private Long getCurrentUserId(WxUserDetails userDetails) {
        if (userDetails == null || userDetails.getWxUser() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        return userDetails.getWxUser().getId();
    }
}