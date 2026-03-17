package com.example.cns.recipe;

import com.example.cns.User.UserEntity;
import com.example.cns.ingredient.IngredientMasterRepository;
import com.example.cns.recipeingredient.RecipeIngredient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeDraftService {

    private final RecipeRepository recipeRepository;
    private final IngredientMasterRepository ingredientMasterRepository;

    @Transactional
    public Long createDraftTransactional(RecipeDTO dto, UserEntity user) {
        Recipe entity = dto.toEntityDraftSafe();
        if (entity.getRecipeType() == null) entity.setRecipeType(RecipeType.IMAGE);
        entity.setUser(user);
        entity.setDraft(true);
        entity.setPublic(false);
        entity.setCreatedAt(LocalDateTime.now());

        recipeRepository.save(entity);

        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            List<RecipeIngredient> ingList = dto.getIngredients().stream()
                    .map(riDto -> {
                        if (riDto.getId() == null && (riDto.getName() == null || riDto.getName().isBlank())) return null;

                        var master = (riDto.getId() != null)
                                ? ingredientMasterRepository.findById(riDto.getId()).orElse(null)
                                : ingredientMasterRepository.findByNameKoIgnoreCase(riDto.getName()).orElse(null);
                        if (master == null) return null;

                        Double qty = riDto.getAmount() != null ? riDto.getAmount() : 1.0;
                        return RecipeIngredient.builder()
                                .ingredient(master)
                                .quantity(qty)
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!ingList.isEmpty()) {
                entity.getIngredients().clear();
                for (RecipeIngredient ri : ingList) {
                    ri.setRecipe(entity);
                    entity.getIngredients().add(ri);
                }
                recipeRepository.save(entity);
            }
        }

        return entity.getRecipeId();
    }

    @Transactional(readOnly = true)
    public RecipeDTO getMyDraftById(Long recipeId, UserEntity user) {
        Recipe recipe = recipeRepository.findDraftWithIngredients(recipeId, user.getId())
                .orElseThrow(() -> new RuntimeException("임시저장 레시피를 찾을 수 없습니다."));
        return RecipeDTO.fromEntity(recipe);
    }

    @Transactional
    public Recipe updateDraft(Long id, RecipeDTO dto, int userId) {
        Recipe draft = recipeRepository
                .findByRecipeIdAndUserIdAndIsDraftTrue(id, userId)
                .orElseThrow(() -> new RuntimeException("임시저장이 없거나 권한 없음."));

        if (StringUtils.hasText(dto.getTitle())) draft.setTitle(dto.getTitle());

        if (StringUtils.hasText(dto.getCategory())) {
            try { draft.setCategory(RecipeCategory.valueOf(dto.getCategory())); }
            catch (Exception e) { log.warn("Invalid category: {}", dto.getCategory()); }
        }
        if (StringUtils.hasText(dto.getDifficulty())) {
            try { draft.setDifficulty(RecipeDifficulty.valueOf(dto.getDifficulty())); }
            catch (Exception e) { log.warn("Invalid difficulty: {}", dto.getDifficulty()); }
        }
        if (StringUtils.hasText(dto.getRecipeType())) {
            try { draft.setRecipeType(RecipeType.valueOf(dto.getRecipeType())); }
            catch (Exception e) { log.warn("Invalid recipeType: {}", dto.getRecipeType()); }
        }

        if (dto.getIngredients() != null) {
            List<RecipeIngredient> ingList = dto.getIngredients().stream()
                    .filter(riDto -> riDto.getId() != null)
                    .map(riDto -> RecipeIngredient.builder()
                            .ingredient(ingredientMasterRepository.findById(riDto.getId())
                                    .orElseThrow(() -> new IllegalArgumentException("재료 없음: " + riDto.getId())))
                            .quantity(riDto.getAmount() != null ? riDto.getAmount() : 1.0)
                            .build())
                    .toList();

            draft.getIngredients().clear();
            for (RecipeIngredient ri : ingList) {
                ri.setRecipe(draft);
                draft.getIngredients().add(ri);
            }
        }

        if (dto.getAlternativeIngredients() != null) draft.setAlternativeIngredients(dto.getAlternativeIngredients());
        if (dto.getHandlingMethods()        != null) draft.setHandlingMethods(dto.getHandlingMethods());
        if (dto.getCookingSteps()           != null) draft.setCookingSteps(dto.getCookingSteps());
        if (dto.getMainImageUrl()           != null) draft.setMainImageUrl(dto.getMainImageUrl());
        if (dto.getTags()                   != null) draft.setTags(dto.getTags());
        if (dto.getVideoUrl()               != null) draft.setVideoUrl(dto.getVideoUrl());
        if (dto.getCookingTime()            != null) draft.setCookingTime(dto.getCookingTime());
        if (dto.getServings()               != null) draft.setServings(dto.getServings());

        draft.setDraft(true);
        draft.setPublic(false);

        return recipeRepository.save(draft);
    }

    @Transactional
    public void deleteDraft(Long id, int userId) {
        Recipe draft = recipeRepository
                .findByRecipeIdAndUserIdAndIsDraftTrue(id, userId)
                .orElseThrow(() -> new RuntimeException("임시저장이 없거나 권한 없음."));
        recipeRepository.delete(draft);
    }

    @Transactional
    public Recipe publishDraft(Long id, boolean isPublic, int userId) {
        Recipe draft = recipeRepository
                .findByRecipeIdAndUserIdAndIsDraftTrue(id, userId)
                .orElseThrow(() -> new RuntimeException("임시저장이 없거나 권한 없음."));

        if (draft.getTitle() == null || draft.getCategory() == null) {
            throw new IllegalArgumentException("제목/카테고리는 필수입니다.");
        }

        draft.setDraft(false);
        draft.setPublic(isPublic);

        return recipeRepository.save(draft);
    }
}
