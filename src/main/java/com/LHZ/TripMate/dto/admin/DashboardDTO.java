package com.LHZ.TripMate.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardDTO {
    private long adminUserCount;
    private long configCount;
}
