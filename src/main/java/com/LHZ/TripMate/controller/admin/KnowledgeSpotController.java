package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.SaveKnowledgeSpotEntryDTO;
import com.LHZ.TripMate.dto.admin.SpotImportResultDTO;
import com.LHZ.TripMate.entity.KnowledgeSpotEntry;
import com.LHZ.TripMate.service.KnowledgeSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 景点结构化知识管理：每个景点ID一条记录，字段独立编辑；
 * 支持整份"景点结构化数据集"docx 导入
 */
@RestController
@RequestMapping("/api/admin/knowledge/spots")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class KnowledgeSpotController {

    private final KnowledgeSpotService knowledgeSpotService;

    @GetMapping
    public Result<List<KnowledgeSpotEntry>> list(
            @RequestParam(required = false) String spotKey,
            @RequestParam(required = false) String zoneName,
            @RequestParam(required = false) String keyword) {
        return Result.success(knowledgeSpotService.list(spotKey, zoneName, keyword));
    }

    @GetMapping("/zones")
    public Result<List<String>> zones() {
        return Result.success(knowledgeSpotService.zoneNames());
    }

    @PostMapping
    public Result<KnowledgeSpotEntry> create(@Valid @RequestBody SaveKnowledgeSpotEntryDTO dto) {
        try {
            return Result.success(knowledgeSpotService.create(dto));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<KnowledgeSpotEntry> update(@PathVariable Long id,
                                             @Valid @RequestBody SaveKnowledgeSpotEntryDTO dto) {
        try {
            return Result.success(knowledgeSpotService.update(id, dto));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            knowledgeSpotService.delete(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/import")
    public Result<SpotImportResultDTO> importDocx(
            @RequestParam("file") MultipartFile file,
            @RequestParam String spotKey) {
        try {
            SpotImportResultDTO result = knowledgeSpotService.importDocx(file, spotKey);
            return Result.success("导入成功", result);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
