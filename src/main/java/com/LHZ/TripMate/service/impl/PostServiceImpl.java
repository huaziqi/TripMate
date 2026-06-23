package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.*;
import com.LHZ.TripMate.entity.*;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.*;
import com.LHZ.TripMate.service.NotificationService;
import com.LHZ.TripMate.service.PostService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepo;
    private final PostLikeRepository likeRepo;
    private final PostFavoriteRepository favoriteRepo;
    private final PostCommentRepository commentRepo;
    private final WxUserRepository wxUserRepo;
    private final UserFollowRepository followRepo;
    private final NotificationService notifService;

    @Override
    @Transactional
    public PostDTO create(PostCreateDTO dto, Long userId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent().trim());
        post.setCategory(dto.getCategory());
        List<String> imgs = dto.getImageUrls() != null ? dto.getImageUrls() : new ArrayList<>();
        post.setImageUrls(imgs);
        post.setCoverUrl(imgs.isEmpty() ? null : imgs.get(0));
        post = postRepo.save(post);
        WxUser user = wxUserRepo.findById(userId).orElseThrow();
        return toDTO(post, user, false, false);
    }

    @Override
    public PageResult<PostDTO> list(String category, String sort, int page, int size, Long currentUserId) {
        Sort s = "hot".equals(sort)
                ? Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.desc("createdAt"));
        Pageable pageable = PageRequest.of(page, size, s);

        Page<Post> posts = (category == null || category.isBlank() || "ALL".equals(category) || "undefined".equals(category))
                ? postRepo.findByStatus("PUBLISHED", pageable)
                : postRepo.findByStatusAndCategory("PUBLISHED", category, pageable);

        List<PostDTO> items = posts.getContent().stream().map(p -> {
            WxUser user = wxUserRepo.findById(p.getUserId()).orElse(null);
            boolean liked = currentUserId != null && likeRepo.existsByPostIdAndUserId(p.getId(), currentUserId);
            boolean faved = currentUserId != null && favoriteRepo.existsByPostIdAndUserId(p.getId(), currentUserId);
            return toDTO(p, user, liked, faved, currentUserId);
        }).toList();

        return new PageResult<>(items, posts.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public PostDTO detail(Long id, Long currentUserId) {
        Post post = postRepo.findById(id)
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        postRepo.incrementViewCount(id);
        post.setViewCount(post.getViewCount() + 1);
        WxUser user = wxUserRepo.findById(post.getUserId()).orElse(null);
        boolean liked = currentUserId != null && likeRepo.existsByPostIdAndUserId(id, currentUserId);
        boolean faved = currentUserId != null && favoriteRepo.existsByPostIdAndUserId(id, currentUserId);
        return toDTO(post, user, liked, faved, currentUserId);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Post post = postRepo.findById(id).orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (!post.getUserId().equals(userId)) throw new RuntimeException("无权删除");
        post.setStatus("DELETED");
        postRepo.save(post);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        Optional<PostLike> existing = likeRepo.findByPostIdAndUserId(postId, userId);
        boolean liked;
        int newCount;
        if (existing.isPresent()) {
            likeRepo.delete(existing.get());
            postRepo.decrementLikeCount(postId);
            liked = false;
            newCount = Math.max(0, post.getLikeCount() - 1);
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeRepo.save(like);
            postRepo.incrementLikeCount(postId);
            liked = true;
            newCount = post.getLikeCount() + 1;
            notifService.create(Notification.Type.LIKE_POST, userId, post.getUserId(),
                    post.getId(), post.getTitle(), null);
        }
        return Map.of("liked", liked, "likeCount", newCount);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleFavorite(Long postId, Long userId) {
        Optional<PostFavorite> existing = favoriteRepo.findByPostIdAndUserId(postId, userId);
        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepo.delete(existing.get());
            favorited = false;
        } else {
            PostFavorite fav = new PostFavorite();
            fav.setPostId(postId);
            fav.setUserId(userId);
            favoriteRepo.save(fav);
            favorited = true;
        }
        return Map.of("favorited", favorited);
    }

    @Override
    public PageResult<PostCommentDTO> listComments(Long postId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        List<PostComment> roots = commentRepo.findByPostIdAndParentIdIsNull(postId, sort);
        long total = roots.size();
        int fromIdx = page * size;
        int toIdx = Math.min(fromIdx + size, roots.size());
        List<PostComment> pageRoots = (fromIdx < roots.size()) ? roots.subList(fromIdx, toIdx) : List.of();

        List<PostCommentDTO> items = pageRoots.stream().map(root -> {
            List<PostComment> children = commentRepo.findByParentId(root.getId(), sort);
            List<PostCommentDTO> replyDTOs = children.stream()
                    .map(c -> toCommentDTO(c, List.of())).toList();
            return toCommentDTO(root, replyDTOs);
        }).toList();
        return new PageResult<>(items, total, page, size);
    }

    @Override
    @Transactional
    public PostCommentDTO addComment(Long postId, CommentCreateDTO dto, Long userId) {
        Post post = postRepo.findById(postId)
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        PostComment c = new PostComment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setContent(dto.getContent().trim());
        c.setParentId(dto.getParentId());
        c = commentRepo.save(c);
        postRepo.incrementCommentCount(postId);
        WxUser user = wxUserRepo.findById(userId).orElse(null);
        notifService.create(Notification.Type.COMMENT_POST, userId, post.getUserId(),
                post.getId(), post.getTitle(), c.getContent());
        return toCommentDTO(c, List.of());
    }

    private PostCommentDTO toCommentDTO(PostComment c, List<PostCommentDTO> replies) {
        WxUser u = wxUserRepo.findById(c.getUserId()).orElse(null);
        return PostCommentDTO.builder()
                .id(c.getId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .author(toAuthorDTO(u))
                .replies(replies)
                .build();
    }

    @Override
    public PageResult<PostDTO> myPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> pg = postRepo.findByStatusAndUserId("PUBLISHED", userId, pageable);
        WxUser user = wxUserRepo.findById(userId).orElse(null);
        List<PostDTO> items = pg.getContent().stream()
                .map(p -> toDTO(p, user, false, false)).toList();
        return new PageResult<>(items, pg.getTotalElements(), page, size);
    }

    @Override
    public PageResult<PostDTO> myFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostFavorite> pg = favoriteRepo.findByUserId(userId, pageable);
        List<PostDTO> items = pg.getContent().stream().map(fav -> {
            Post post = postRepo.findById(fav.getPostId())
                    .filter(p -> "PUBLISHED".equals(p.getStatus())).orElse(null);
            if (post == null) return null;
            WxUser user = wxUserRepo.findById(post.getUserId()).orElse(null);
            return toDTO(post, user, true, true);
        }).filter(Objects::nonNull).toList();
        long effectiveTotal = pg.getTotalElements() - (pg.getNumberOfElements() - items.size());
        return new PageResult<>(items, Math.max(effectiveTotal, 0), page, size);
    }

    @Override
    public PageResult<PostDTO> search(String q, int page, int size, Long currentUserId) {
        if (q == null || q.isBlank()) return new PageResult<>(List.of(), 0, page, size);
        Page<Post> pg = postRepo.search(q.trim(), PageRequest.of(page, size));
        var items = pg.getContent().stream().map(p -> {
            WxUser user = wxUserRepo.findById(p.getUserId()).orElse(null);
            boolean liked = currentUserId != null && likeRepo.existsByPostIdAndUserId(p.getId(), currentUserId);
            boolean faved = currentUserId != null && favoriteRepo.existsByPostIdAndUserId(p.getId(), currentUserId);
            return toDTO(p, user, liked, faved, currentUserId);
        }).toList();
        return new PageResult<>(items, pg.getTotalElements(), page, size);
    }

    // ---- 私有辅助 ----

    private PostDTO toDTO(Post p, WxUser user, boolean liked, boolean favorited) {
        return toDTO(p, user, liked, favorited, null);
    }

    private PostDTO toDTO(Post p, WxUser user, boolean liked, boolean favorited, Long currentUserId) {
        return PostDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .content(p.getContent())
                .category(p.getCategory())
                .coverUrl(p.getCoverUrl())
                .imageUrls(p.getImageUrls())
                .viewCount(p.getViewCount())
                .likeCount(p.getLikeCount())
                .commentCount(p.getCommentCount())
                .createdAt(p.getCreatedAt())
                .author(toAuthorDTO(user, currentUserId))
                .liked(liked)
                .favorited(favorited)
                .build();
    }

    private PostDTO.AuthorDTO toAuthorDTO(WxUser user) {
        return toAuthorDTO(user, null);
    }

    private PostDTO.AuthorDTO toAuthorDTO(WxUser user, Long currentUserId) {
        if (user == null) return PostDTO.AuthorDTO.builder().build();
        boolean following = currentUserId != null &&
                followRepo.existsByFollowerIdAndFollowingId(currentUserId, user.getId());
        return PostDTO.AuthorDTO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .following(following)
                .build();
    }
}
