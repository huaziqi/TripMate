package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.DashboardDTO;
import com.LHZ.TripMate.dto.admin.DashboardOverviewDTO;

public interface DashboardService {
    DashboardDTO getStats();

    /** 数据大屏概览 */
    DashboardOverviewDTO getOverview();
}
