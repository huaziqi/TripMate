package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PostCommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private PostDTO.AuthorDTO author;
}
