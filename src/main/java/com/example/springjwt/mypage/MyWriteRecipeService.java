package com.example.springjwt.mypage;

import java.util.List;

public interface MyWriteRecipeService {
    List<MyWriteRecipeDTO> getMyRecipes(int userId, String sort, List<String> categoryFilters, List<String> ingredientFilters);
    MyWriteRecipeDTO getRecipeDetail(int recipeId, int userId);
    void deleteMyRecipe(int recipeId, int userId);
}
