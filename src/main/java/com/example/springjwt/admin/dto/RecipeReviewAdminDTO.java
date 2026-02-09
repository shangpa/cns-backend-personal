package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RecipeReviewAdminDTO {
    private Long reviewId;
    private String username;
    private String content;
    private int rating;
    private String mediaUrls;
    private LocalDateTime createdAt;
}
