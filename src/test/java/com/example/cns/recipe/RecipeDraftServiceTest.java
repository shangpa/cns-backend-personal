package com.example.cns.recipe;

import com.example.cns.User.UserEntity;
import com.example.cns.ingredient.IngredientMasterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeDraftServiceTest {

    @Mock RecipeRepository recipeRepository;
    @Mock IngredientMasterRepository ingredientMasterRepository;

    @InjectMocks
    RecipeDraftService recipeDraftService;

    @Test
    void createDraft_success() {
        // given
        RecipeDTO dto = RecipeDTO.builder()
                .title("임시 레시피")
                .build();

        UserEntity user = new UserEntity();
        user.setId(1);

        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe r = invocation.getArgument(0);
            r.setRecipeId(10L);
            return r;
        });

        // when
        Long id = recipeDraftService.createDraftTransactional(dto, user);

        // then
        assertThat(id).isEqualTo(10L);
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void getMyDraftById_unauthorized_throwsException() {
        // given
        UserEntity user = new UserEntity();
        user.setId(1);

        when(recipeRepository.findDraftWithIngredients(99L, 1))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> recipeDraftService.getMyDraftById(99L, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("임시저장 레시피를 찾을 수 없습니다");
    }
}
