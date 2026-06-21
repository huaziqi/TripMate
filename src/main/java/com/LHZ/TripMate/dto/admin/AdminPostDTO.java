package com.LHZ.TripMate.dto.admin;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminPostDTO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String coverUrl;
    private int likeCount;
    private int commentCount;
    private int viewCount;
    private String status;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorNickname;
}
