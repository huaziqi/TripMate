package com.LHZ.TripMate.dto;

import lombok.Data;
import java.util.List;

@Data
public class PostCreateDTO {
    private String title;
    private String content;
    private String category;
    private List<String> imageUrls;
}
