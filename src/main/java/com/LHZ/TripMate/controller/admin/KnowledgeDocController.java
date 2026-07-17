package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.KnowledgeDocItemDTO;
import com.LHZ.TripMate.dto.admin.SaveKnowledgeDocDTO;
import com.LHZ.TripMate.entity.KnowledgeDoc;
import com.LHZ.TripMate.service.KnowledgeDocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理：管理员上传、更新和维护景区的讲解词、文史资料、常见问题等知识文档
 */
@RestController
@RequestMapping("/api/admin/knowledge")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class KnowledgeDocController {

    private final KnowledgeDocService knowledgeDocService;

    @GetMapping
    public Result<List<KnowledgeDocItemDTO>> list(
            @RequestParam(required = false) String spotKey,
            @RequestParam(required = false) KnowledgeDoc.Category category,
            @RequestParam(required = false) String keyword) {
        return Result.success(knowledgeDocService.list(spotKey, category, keyword));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDoc> get(@PathVariable Long id) {
        try {
            return Result.success(knowledgeDocService.get(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping
    public Result<KnowledgeDoc> create(@Valid @RequestBody SaveKnowledgeDocDTO dto) {
        return Result.success(knowledgeDocService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeDoc> update(@PathVariable Long id,
                                       @Valid @RequestBody SaveKnowledgeDocDTO dto) {
        try {
            return Result.success(knowledgeDocService.update(id, dto));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            knowledgeDocService.delete(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result<KnowledgeDoc> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String spotKey,
            @RequestParam(required = false) KnowledgeDoc.Category category,
            @RequestParam(required = false) String title) {
        try {
            return Result.success("上传成功", knowledgeDocService.importFile(file, spotKey, category, title));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
