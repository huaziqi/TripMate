package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.DashboardDTO;
import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.repository.SystemConfigRepository;
import com.LHZ.TripMate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AdminUserRepository adminUserRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Override
    public DashboardDTO getStats() {
        return new DashboardDTO(
                adminUserRepository.count(),
                systemConfigRepository.count()
        );
    }
}
