package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.dto.admin.AdminPostDTO;
import com.LHZ.TripMate.entity.Post;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.PostRepository;
import com.LHZ.TripMate.repository.WxUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostRepository postRepo;
    private final WxUserRepository wxUserRepo;

    @GetMapping
    public Result<PageResult<AdminPostDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> pg = (status != null && !status.isBlank())
                ? postRepo.findByStatus(status, pageable)
                : postRepo.findAll(pageable);
        var items = pg.getContent().stream().map(p -> {
            WxUser u = wxUserRepo.findById(p.getUserId()).orElse(null);
            String preview = p.getContent() != null && p.getContent().length() > 200
                    ? p.getContent().substring(0, 200) : p.getContent();
            return AdminPostDTO.builder()
                    .id(p.getId()).title(p.getTitle()).content(preview)
                    .category(p.getCategory()).coverUrl(p.getCoverUrl())
                    .likeCount(p.getLikeCount()).commentCount(p.getCommentCount())
                    .viewCount(p.getViewCount()).status(p.getStatus())
                    .createdAt(p.getCreatedAt())
                    .authorId(u != null ? u.getId() : null)
                    .authorNickname(u != null ? u.getNickname() : "未知用户")
                    .build();
        }).toList();
        return Result.success(new PageResult<>(items, pg.getTotalElements(), page, size));
    }

    @PostMapping("/{id}/delete")
    public Result<Void> deletePost(@PathVariable Long id) {
        postRepo.findById(id).ifPresent(p -> {
            p.setStatus("DELETED");
            postRepo.save(p);
        });
        return Result.success(null);
    }

    @PostMapping("/{id}/restore")
    public Result<Void> restorePost(@PathVariable Long id) {
        postRepo.findById(id).ifPresent(p -> {
            p.setStatus("PUBLISHED");
            postRepo.save(p);
        });
        return Result.success(null);
    }
}
