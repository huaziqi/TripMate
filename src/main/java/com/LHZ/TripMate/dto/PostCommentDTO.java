package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostCommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private PostDTO.AuthorDTO author;
    private List<PostCommentDTO> replies;
}
