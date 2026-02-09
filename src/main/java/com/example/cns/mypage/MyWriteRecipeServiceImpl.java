package com.example.cns.mypage;

import com.example.cns.recipe.Recipe;
import com.example.cns.recipe.RecipeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyWriteRecipeServiceImpl implements MyWriteRecipeService {

    private final RecipeRepository recipeRepository;

    @Override
    @Transactional
    public List<MyWriteRecipeDTO> getMyRecipes(int userId, String sort, List<String> categoryFilters, List<String> ingredientFilters) {
        List<Recipe> recipes;

        if ((categoryFilters == null || categoryFilters.isEmpty()) && (ingredientFilters == null || ingredientFilters.isEmpty())) {
            // 🔸 필터 없음
            recipes = getSortedRecipes(recipeRepository.findByUserId(userId), sort);
        } else {
            // 🔸 필터 존재
            recipes = getSortedRecipes(
                    recipeRepository.findByUserId(userId).stream()
                            .filter(recipe -> {
                                boolean categoryMatch = categoryFilters == null || categoryFilters.isEmpty() ||
                                        categoryFilters.contains(recipe.getCategory().name());
                                boolean ingredientMatch = ingredientFilters == null || ingredientFilters.isEmpty() ||
                                        ingredientFilters.stream().anyMatch(keyword -> recipe.getIngredients() != null && recipe.getIngredients().contains(keyword));
                                return categoryMatch && ingredientMatch;
                            })
                            .collect(Collectors.toList()),
                    sort
            );
        }

        return recipes.stream().map(MyWriteRecipeDTO::new).collect(Collectors.toList());
    }

    private List<Recipe> getSortedRecipes(List<Recipe> recipes, String sort) {
        if ("views".equalsIgnoreCase(sort)) {
            return recipes.stream()
                    .sorted((a, b) -> Integer.compare(b.getViewCount(), a.getViewCount()))
                    .collect(Collectors.toList());
        } else {
            return recipes.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional
    public void deleteMyRecipe(int recipeId, int userId) {
        Recipe recipe = recipeRepository.findById((long) recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피가 존재하지 않습니다."));
        if (recipe.getUser().getId() != userId) {
            throw new SecurityException("본인이 작성한 레시피만 삭제할 수 있습니다.");
        }
        recipeRepository.delete(recipe);
    }

    @Override
    @Transactional
    public MyWriteRecipeDTO getRecipeDetail(int recipeId, int userId) {
        Recipe recipe = recipeRepository.findById((long) recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피가 존재하지 않습니다."));
        if (recipe.getUser().getId() != userId) {
            throw new SecurityException("본인의 레시피만 조회할 수 있습니다.");
        }
        return new MyWriteRecipeDTO(recipe);
    }
}
