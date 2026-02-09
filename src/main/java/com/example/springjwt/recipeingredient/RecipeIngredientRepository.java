package com.example.springjwt.recipeingredient;

import com.example.springjwt.recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    // 레시피별 재료 조회
    List<RecipeIngredient> findByRecipe(Recipe recipe);

    // 레시피 ID 기반 조회
    List<RecipeIngredient> findByRecipe_RecipeId(Long recipeId);

    // 필요하다면 재료 ID 기반 조회
    List<RecipeIngredient> findByIngredient_Id(Long ingredientId);
}
