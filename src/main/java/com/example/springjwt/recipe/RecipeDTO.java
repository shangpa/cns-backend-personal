package com.example.springjwt.recipe;

import com.example.springjwt.recipeingredient.RecipeIngredientDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeDTO {
    private Long recipeId;
    private String title;
    private String category;                 // RecipeCategory.name()
    private List<RecipeIngredientDTO> ingredients;             // JSON
    private String alternativeIngredients;   // JSON
    private String handlingMethods;          // JSON
    private String cookingSteps;             // JSON
    private String mainImageUrl;
    private String difficulty;               // RecipeDifficulty.name()  // "초급","중급","상급"
    private String tags;

    // ★ null 허용 (Integer/Boolean 래퍼)
    private Integer cookingTime;             // null이면 미입력
    private Integer servings;                // null이면 미입력
    private LocalDateTime createdAt;

    @JsonProperty("isPublic")
    private Boolean isPublic;                // null이면 미입력

    private String writer;
    private String videoUrl;

    @Builder.Default
    private String recipeType = "IMAGE";     // IMAGE / VIDEO / BOTH

    @JsonProperty("isDraft")
    private Boolean isDraft;                 // null이면 미입력

    /** Entity -> DTO */
    public static RecipeDTO fromEntity(Recipe recipe) {
        return RecipeDTO.builder()
                .recipeId(recipe.getRecipeId())
                .title(recipe.getTitle())
                .category(recipe.getCategory() != null ? recipe.getCategory().name() : null)
                .ingredients(recipe.getIngredients() != null ?
                        recipe.getIngredients().stream()
                                .map(RecipeIngredientDTO::fromEntity)
                                .toList()
                        : null)
                .alternativeIngredients(recipe.getAlternativeIngredients())
                .handlingMethods(recipe.getHandlingMethods())
                .cookingSteps(recipe.getCookingSteps())
                .mainImageUrl(recipe.getMainImageUrl())
                .difficulty(recipe.getDifficulty() != null ? recipe.getDifficulty().name() : null)
                .tags(recipe.getTags())
                .cookingTime(recipe.getCookingTime())   // 엔티티도 Integer 권장
                .servings(recipe.getServings())
                .createdAt(recipe.getCreatedAt())
                .isPublic(recipe.isPublic())            // 엔티티가 primitive면 박싱됨
                .writer(recipe.getUser() != null ? recipe.getUser().getUsername() : null)
                .videoUrl(recipe.getVideoUrl())
                .recipeType(recipe.getRecipeType() != null ? recipe.getRecipeType().name() : "IMAGE")
                .isDraft(recipe.isDraft())
                .build();
    }

    /** 발행용(정식 레시피) 변환: 기본값 보정 포함 */
    public Recipe toEntity() {
        Recipe.RecipeBuilder b = Recipe.builder()
                .title(this.title)
                .alternativeIngredients(this.alternativeIngredients)
                .handlingMethods(this.handlingMethods)
                .cookingSteps(this.cookingSteps)
                .mainImageUrl(this.mainImageUrl)
                .tags(this.tags)
                .videoUrl(this.videoUrl)
                .createdAt(this.createdAt != null ? this.createdAt : LocalDateTime.now());

        if (this.category != null) {
            try { b.category(RecipeCategory.valueOf(this.category)); } catch (Exception ignored) {}
        }
        if (this.difficulty != null) {
            try { b.difficulty(RecipeDifficulty.valueOf(this.difficulty)); } catch (Exception ignored) {}
        }
        if (this.recipeType != null) {
            try { b.recipeType(RecipeType.valueOf(this.recipeType)); } catch (Exception ignored) {}
        }

        // 숫자/불린 기본값 보정
        b.cookingTime(this.cookingTime != null ? this.cookingTime : 0);
        b.servings(this.servings != null ? this.servings : 0);
        b.isPublic(Boolean.TRUE.equals(this.isPublic));
        b.isDraft(Boolean.TRUE.equals(this.isDraft));

        return b.build();
    }

    /** 임시저장용(초안) 변환: null/빈값 안전하게만 세팅 */
    public Recipe toEntityDraftSafe() {
        Recipe r = new Recipe();
        if (this.title != null && !this.title.isBlank()) r.setTitle(this.title);
        if (this.category != null && !this.category.isBlank()) {
            try { r.setCategory(RecipeCategory.valueOf(this.category)); } catch (Exception ignored) {}
        }
        if (this.alternativeIngredients != null) r.setAlternativeIngredients(this.alternativeIngredients);
        if (this.handlingMethods != null) r.setHandlingMethods(this.handlingMethods);
        if (this.cookingSteps != null) r.setCookingSteps(this.cookingSteps);
        if (this.mainImageUrl != null) r.setMainImageUrl(this.mainImageUrl);
        if (this.difficulty != null && !this.difficulty.isBlank()) {
            try { r.setDifficulty(RecipeDifficulty.valueOf(this.difficulty)); } catch (Exception ignored) {}
        }
        if (this.tags != null) r.setTags(this.tags);
        if (this.cookingTime != null) r.setCookingTime(this.cookingTime);
        if (this.servings != null) r.setServings(this.servings);
        if (this.videoUrl != null) r.setVideoUrl(this.videoUrl);
        if (this.recipeType != null && !this.recipeType.isBlank()) {
            try { r.setRecipeType(RecipeType.valueOf(this.recipeType)); } catch (Exception ignored) {}
        }
        // draft/public/user/createdAt은 컨트롤러에서 강제 세팅
        return r;
    }
}
