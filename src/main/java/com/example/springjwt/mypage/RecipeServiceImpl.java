package com.example.springjwt.mypage;

import com.example.springjwt.recipe.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements MyWriteRecipeService {

    private final RecipeRepository recipeRepository;

    @Override
    public List<MyWriteRecipeDTO> getMyRecipes(int userId, String sort, List<String> categoryFilters, List<String> ingredientFilters) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId).stream()
                .filter(recipe -> {
                    boolean categoryMatch = categoryFilters == null || categoryFilters.isEmpty()
                            || categoryFilters.contains(recipe.getCategory().name());
                    boolean ingredientMatch = ingredientFilters == null || ingredientFilters.isEmpty()
                            || ingredientFilters.stream().anyMatch(keyword ->
                            recipe.getIngredients() != null && recipe.getIngredients().contains(keyword)
                    );
                    return categoryMatch && ingredientMatch;
                })
                .collect(Collectors.toList());

        return recipes.stream()
                .map(MyWriteRecipeDTO::new)
                .collect(Collectors.toList());
    }


    @Override
    public MyWriteRecipeDTO getRecipeDetail(int recipeId, int userId) {
        Recipe recipe = recipeRepository.findByRecipeIdAndUserId((long) recipeId, userId)
                .orElseThrow(() -> new RuntimeException("해당 레시피를 찾을 수 없습니다."));
        return MyWriteRecipeDTO.fromEntity(recipe);
    }

    @Override
    public void deleteMyRecipe(int recipeId, int userId) {
        Recipe recipe = recipeRepository.findByRecipeIdAndUserId((long) recipeId, userId)
                .orElseThrow(() -> new RuntimeException("삭제할 레시피를 찾을 수 없습니다."));

        if (recipe.getUser().getId() != userId) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        recipeRepository.delete(recipe);
    }
}
