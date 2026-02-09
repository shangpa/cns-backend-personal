package com.example.springjwt.search;

public record SeasonalRecipeDto(
        Long recipeId,
        String title,
        String mainImageUrl
) {}