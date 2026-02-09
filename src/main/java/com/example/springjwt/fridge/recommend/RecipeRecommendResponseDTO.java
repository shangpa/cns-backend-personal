package com.example.springjwt.fridge.recommend;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeRecommendResponseDTO {
    private Long recipeId;
    private String title;
    private String mainImageUrl;
    private String difficulty;
    private int cookingTime;
    private double reviewAverage;
    private int reviewCount;
    private String writerNickname;
    private int viewCount;
    private int likeCount;
    private String createdAt;
}