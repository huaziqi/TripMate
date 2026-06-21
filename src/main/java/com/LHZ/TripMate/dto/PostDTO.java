package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String coverUrl;
    private List<String> imageUrls;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private AuthorDTO author;
    private boolean liked;
    private boolean favorited;

    @Data @Builder
    public static class AuthorDTO {
        private Long id;
        private String nickname;
        private String avatarUrl;
        private boolean following;
    }
}
