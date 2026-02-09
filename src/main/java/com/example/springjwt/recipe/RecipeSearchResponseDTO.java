package com.example.springjwt.recipe;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeSearchResponseDTO {
    private Long recipeId;
    private String title;
    private String mainImageUrl;
    private String difficulty;
    private int cookingTime;
    private Map<String, Object> user;
    private String category;
    private String createdAt;

    private String videoUrl;
    private int viewCount;
    private int likes;

    private double averageRating;//평균 리뷰 점수
    private int reviewCount; //리뷰수
    private boolean liked; // 현재 로그인한 사용자가 찜했는지 여부

    public static RecipeSearchResponseDTO fromEntity(Recipe recipe, double avgRating, int reviewCount, boolean liked) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", recipe.getUser().getId());
        userMap.put("name", recipe.getUser().getName());

        return RecipeSearchResponseDTO.builder()
                .recipeId(recipe.getRecipeId())
                .title(recipe.getTitle())
                .mainImageUrl(recipe.getMainImageUrl())
                .difficulty(recipe.getDifficulty().name())
                .cookingTime(recipe.getCookingTime())
                .user(userMap)
                .videoUrl(recipe.getVideoUrl())
                .category(recipe.getCategory().name())
                .createdAt(recipe.getCreatedAt().toString())
                .viewCount(recipe.getViewCount())
                .likes(recipe.getLikes())
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .liked(liked)
                .build();
    }
}


