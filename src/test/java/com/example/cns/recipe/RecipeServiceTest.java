package com.example.cns.recipe;

import com.example.cns.User.UserEntity;
import com.example.cns.User.UserRepository;
import com.example.cns.admin.log.AdminLogService;
import com.example.cns.api.OpenAiService;
import com.example.cns.api.ThumbnailAsyncService;
import com.example.cns.api.vision.IngredientParser;
import com.example.cns.fridge.FridgeRepository;
import com.example.cns.ingredient.IngredientMasterRepository;
import com.example.cns.mypage.LikeRecipeRepository;
import com.example.cns.mypage.RecommendRecipeRepository;
import com.example.cns.point.PointActionType;
import com.example.cns.point.PointService;
import com.example.cns.recipe.cashe.IngredientNameCache;
import com.example.cns.recipeingredient.RecipeIngredientRepository;
import com.example.cns.review.Recipe.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock RecipeRepository recipeRepository;
    @Mock UserRepository userRepository;
    @Mock PointService pointService;
    @Mock LikeRecipeRepository likeRecipeRepository;
    @Mock FridgeRepository fridgeRepository;
    @Mock AdminLogService adminLogService;
    @Mock RecommendRecipeRepository recommendRecipeRepository;
    @Mock IngredientNameCache ingredientNameCache;
    @Mock IngredientParser ingredientParser;
    @Mock OpenAiService openAiService;
    @Mock IngredientMasterRepository ingredientMasterRepository;
    @Mock RecipeIngredientRepository recipeIngredientRepository;
    @Mock ThumbnailAsyncService thumbnailAsyncService;

    @InjectMocks
    RecipeService recipeService;

    @Test
    void createRecipe_success() {
        // given
        RecipeDTO dto = RecipeDTO.builder()
                .title("된장찌개")
                .category("koreaFood")
                .difficulty("EASY")
                .isDraft(false)
                .isPublic(true)
                .build();

        UserEntity user = new UserEntity();
        when(userRepository.findByUsername("user1")).thenReturn(user);

        Recipe savedRecipe = Recipe.builder()
                .recipeId(1L)
                .title("된장찌개")
                .build();
        when(recipeRepository.save(any(Recipe.class))).thenReturn(savedRecipe);

        // when
        Recipe result = recipeService.createRecipe(dto, "user1");

        // then
        assertThat(result.getRecipeId()).isEqualTo(1L);
        verify(pointService).addPoint(eq(user), eq(PointActionType.RECIPE_WRITE), eq(1), any());
    }

    @Test
    void getRecipeById_notFound_throwsException() {
        // given
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> recipeService.getRecipeById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("레시피를 찾을 수 없습니다");
    }
}
