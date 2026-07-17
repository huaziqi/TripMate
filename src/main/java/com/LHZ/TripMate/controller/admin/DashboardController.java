package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.DashboardDTO;
import com.LHZ.TripMate.dto.admin.DashboardOverviewDTO;
import com.LHZ.TripMate.dto.admin.VisitorImportResultDTO;
import com.LHZ.TripMate.service.DashboardService;
import com.LHZ.TripMate.service.VisitorStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final VisitorStatsService visitorStatsService;

    @GetMapping
    public Result<DashboardDTO> stats() {
        return Result.success(dashboardService.getStats());
    }

    /** 数据大屏概览：服务人次、热门问答、满意度趋势等 */
    @GetMapping("/overview")
    public Result<DashboardOverviewDTO> overview() {
        return Result.success(dashboardService.getOverview());
    }

    /** 导入游客行为分析数据 xlsx（覆盖旧数据） */
    @PostMapping("/import-visitors")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<VisitorImportResultDTO> importVisitors(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success("导入成功", visitorStatsService.importXlsx(file));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
