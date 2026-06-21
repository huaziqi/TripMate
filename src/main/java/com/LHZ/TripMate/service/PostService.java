package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.*;

import java.util.Map;

public interface PostService {
    PostDTO create(PostCreateDTO dto, Long userId);
    PageResult<PostDTO> list(String category, String sort, int page, int size, Long currentUserId);
    PostDTO detail(Long id, Long currentUserId);
    void delete(Long id, Long userId);

    Map<String, Object> toggleLike(Long postId, Long userId);
    Map<String, Object> toggleFavorite(Long postId, Long userId);

    PageResult<PostCommentDTO> listComments(Long postId, int page, int size);
    PostCommentDTO addComment(Long postId, CommentCreateDTO dto, Long userId);

    PageResult<PostDTO> myPosts(Long userId, int page, int size);
    PageResult<PostDTO> myFavorites(Long userId, int page, int size);

    PageResult<PostDTO> search(String q, int page, int size, Long currentUserId);
}
