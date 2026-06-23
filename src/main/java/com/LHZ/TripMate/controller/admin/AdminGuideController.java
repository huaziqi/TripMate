package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.GuideSpotConfigDTO;
import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/guide")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminGuideController {

    private final GuideSpotConfigRepository configRepo;

    @GetMapping("/configs")
    public Result<List<GuideSpotConfig>> list() {
        return Result.success(configRepo.findAll());
    }

    @PutMapping("/configs/{spotKey}")
    public Result<GuideSpotConfig> upsert(
            @PathVariable String spotKey,
            @RequestBody GuideSpotConfigDTO dto) {

        GuideSpotConfig config = configRepo.findBySpotKey(spotKey)
                .orElseGet(GuideSpotConfig::new);
        config.setSpotKey(spotKey);
        config.setPersonaName(dto.getPersonaName());
        config.setPersonaDesc(dto.getPersonaDesc());
        config.setKnowledgeText(dto.getKnowledgeText());
        config.setActive(dto.isActive());
        configRepo.save(config);
        return Result.success(config);
    }
}
