package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.*;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public Result<PostDTO> create(@RequestBody PostCreateDTO dto,
                                  @AuthenticationPrincipal WxUserDetails userDetails) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) return Result.fail("标题不能为空");
        if (dto.getContent() == null || dto.getContent().isBlank()) return Result.fail("内容不能为空");
        if (dto.getCategory() == null || dto.getCategory().isBlank()) return Result.fail("请选择分类");
        try {
            return Result.success(postService.create(dto, userDetails.getWxUser().getId()));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping
    public Result<PageResult<PostDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getWxUser().getId() : null;
        return Result.success(postService.list(category, sort, page, size, userId));
    }

    @GetMapping("/my")
    public Result<PageResult<PostDTO>> myPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.myPosts(userDetails.getWxUser().getId(), page, size));
    }

    @GetMapping("/my/favorites")
    public Result<PageResult<PostDTO>> myFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.myFavorites(userDetails.getWxUser().getId(), page, size));
    }

    @GetMapping("/{id}")
    public Result<PostDTO> detail(@PathVariable Long id,
                                  @AuthenticationPrincipal WxUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getWxUser().getId() : null;
        try {
            return Result.success(postService.detail(id, userId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal WxUserDetails userDetails) {
        try {
            postService.delete(id, userDetails.getWxUser().getId());
            return Result.success();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable Long id,
                                            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.toggleLike(id, userDetails.getWxUser().getId()));
    }

    @PostMapping("/{id}/favorite")
    public Result<Map<String, Object>> favorite(@PathVariable Long id,
                                                @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.toggleFavorite(id, userDetails.getWxUser().getId()));
    }

    @GetMapping("/{id}/comments")
    public Result<PageResult<PostCommentDTO>> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(postService.listComments(id, page, size));
    }

    @PostMapping("/{id}/comments")
    public Result<PostCommentDTO> addComment(@PathVariable Long id,
                                             @RequestBody CommentCreateDTO dto,
                                             @AuthenticationPrincipal WxUserDetails userDetails) {
        if (dto.getContent() == null || dto.getContent().isBlank()) return Result.fail("评论不能为空");
        if (dto.getContent().length() > 500) return Result.fail("评论最多 500 字");
        try {
            return Result.success(postService.addComment(id, dto.getContent(), userDetails.getWxUser().getId()));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
