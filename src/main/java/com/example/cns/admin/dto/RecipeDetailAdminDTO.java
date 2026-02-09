package com.example.cns.admin.dto;

import com.example.cns.recipe.RecipeDifficulty;
import com.example.cns.recipe.RecipeCategory;
import com.example.cns.recipeingredient.RecipeIngredientDTO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RecipeDetailAdminDTO {
    private Long recipeId;
    private String username;
    private String title;
    private RecipeCategory category;
    private List<RecipeIngredientDTO> ingredients;
    private String alternativeIngredients;
    private String handlingMethods;
    private String cookingSteps;
    private String mainImageUrl;
    private RecipeDifficulty difficulty;
    private String tags;
    private int cookingTime;
    private int servings;
    private LocalDateTime createdAt;
    private boolean isPublic;
    private int viewCount;
    private int likes;
    private int recommends;
    private String videoUrl;
    private List<RecipeReviewAdminDTO> reviews; // 리뷰 리스트
}
