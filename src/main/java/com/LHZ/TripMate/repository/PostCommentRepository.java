package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    Page<PostComment> findByPostId(Long postId, Pageable pageable);
    List<PostComment> findByPostIdAndParentIdIsNull(Long postId, Sort sort);
    List<PostComment> findByParentId(Long parentId, Sort sort);
}
