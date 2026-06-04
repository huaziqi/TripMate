package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.entity.SystemConfig;
import com.LHZ.TripMate.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<List<SystemConfig>> list() {
        return Result.success(systemConfigService.listAll());
    }

    @PutMapping("/{key}")
    public Result<SystemConfig> update(@PathVariable String key,
                                       @RequestBody Map<String, String> body) {
        return Result.success(systemConfigService.update(key, body.get("value")));
    }
}
