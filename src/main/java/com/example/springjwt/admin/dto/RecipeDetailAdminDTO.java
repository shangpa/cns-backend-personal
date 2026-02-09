package com.example.springjwt.admin.dto;

import com.example.springjwt.recipe.RecipeDifficulty;
import com.example.springjwt.recipe.RecipeCategory;
import com.example.springjwt.recipeingredient.RecipeIngredientDTO;
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
