package com.example.springjwt.fridge.recommend;
import com.example.springjwt.mypage.LikeRecipeRepository;
import com.example.springjwt.recipe.Recipe;
import com.example.springjwt.recipe.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FridgeRecommendService {

    private final RecipeRepository recipeRepository;
    private final LikeRecipeRepository likeRecipeRepository;
    private final ObjectMapper objectMapper;

    private boolean isRecipeMatch(Recipe recipe, List<String> selectedIngredients) {
        if (recipe.getIngredients() == null) return false;

        List<String> ingredientNames = recipe.getIngredients().stream()
                .map(ri -> ri.getIngredient().getNameKo().replaceAll("\\s+", ""))
                .toList();

        return selectedIngredients.stream()
                .map(name -> name.replaceAll("\\s+", ""))
                .allMatch(ingredientNames::contains);
    }

    // id로 넘어온 재료 레시피 추천
    public List<RecipeRecommendResponseDTO> recommendRecipes(List<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return List.of();
        }


        List<Recipe> recipes = recipeRepository.findPublicRecipesContainingAnyIngredientIds(ingredientIds);

        List<RecipeRecommendResponseDTO> result = new ArrayList<>(recipes.size());
        for (Recipe r : recipes) {
            int likeCount = likeRecipeRepository.countByRecipe(r); // (옵션) 배치 최적화 가능
            result.add(RecipeRecommendResponseDTO.builder()
                    .recipeId(r.getRecipeId())
                    .title(r.getTitle())
                    .mainImageUrl(r.getMainImageUrl())
                    .difficulty(r.getDifficulty() != null ? r.getDifficulty().name() : null)
                    .cookingTime(r.getCookingTime())
                    .reviewAverage(0.0) // TODO: 리뷰 평균 연동
                    .reviewCount(0)     // TODO: 리뷰 수 연동
                    .writerNickname(r.getUser() != null ? r.getUser().getUsername() : null)
                    .viewCount(r.getViewCount())
                    .likeCount(likeCount)
                    .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                    .build());
        }
        return result;
    }

}