package com.example.cns.recipe;

import com.example.cns.admin.log.AdminLogService;
import com.example.cns.api.OpenAiService;
import com.example.cns.api.ThumbnailAsyncService;
import com.example.cns.api.vision.IngredientParser;
import com.example.cns.fridge.Fridge;
import com.example.cns.fridge.FridgeRepository;
import com.example.cns.ingredient.IngredientMasterRepository;
import com.example.cns.mypage.LikeRecipeRepository;
import com.example.cns.User.UserEntity;
import com.example.cns.User.UserRepository;
import com.example.cns.mypage.RecommendRecipeRepository;
import com.example.cns.point.PointActionType;
import com.example.cns.point.PointService;
import com.example.cns.recipe.cashe.IngredientNameCache;
import com.example.cns.recipe.expected.ExpectedIngredientDTO;
import com.example.cns.recipeingredient.RecipeIngredient;
import com.example.cns.recipeingredient.RecipeIngredientRepository;
import com.example.cns.review.Recipe.ReviewRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final ReviewRepository reviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final LikeRecipeRepository likeRecipeRepository;
    private final FridgeRepository fridgeRepository;
    private final AdminLogService adminLogService;
    private final RecommendRecipeRepository recommendRecipeRepository;
    private final IngredientNameCache ingredientNameCache;
    private final IngredientParser ingredientParser;
    private final OpenAiService openAiService;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;

    private static final int SUGGEST_LIMIT = 3;

    private final ThumbnailAsyncService thumbnailAsyncService;
    // 전체 레시피 조회
    @Transactional(readOnly = true)
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    // 공개된 레시피만 정렬해서 가져오기 (비로그인 가드 포함)
    @Transactional(readOnly = true)
    public List<RecipeSearchResponseDTO> getAllPublicRecipes(String sort) {
        List<Recipe> recipes;

        String username = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(a -> a.getName()).orElse(null);
        UserEntity currentUser = (username != null && !"anonymousUser".equalsIgnoreCase(username))
                ? userRepository.findByUsername(username)
                : null;

        switch (sort != null ? sort : "viewCount") {
            case "likes"     -> recipes = recipeRepository.findByIsPublicTrueAndIsDraftFalseOrderByLikesDesc();
            case "latest"    -> recipes = recipeRepository.findByIsPublicTrueAndIsDraftFalseOrderByCreatedAtDesc();
            case "shortTime" -> recipes = recipeRepository.findByIsPublicTrueAndIsDraftFalseOrderByCookingTimeAsc();
            case "longTime"  -> recipes = recipeRepository.findByIsPublicTrueAndIsDraftFalseOrderByCookingTimeDesc();
            case "viewCount" -> recipes = recipeRepository.findByIsPublicTrueAndIsDraftFalseOrderByViewCountDesc();
            default          -> recipes = recipeRepository.findByIsPublicTrueAndIsDraftFalseOrderByViewCountDesc();
        }

        return recipes.stream().map(recipe -> {
            Double avgRatingWrapper = reviewRepository.findAvgRatingByRecipe(recipe.getRecipeId());
            double avgRating = avgRatingWrapper != null ? avgRatingWrapper : 0.0;
            int reviewCount = reviewRepository.countByRecipe(recipe);
            boolean liked = (currentUser != null) && likeRecipeRepository.existsByUserAndRecipe(currentUser, recipe);
            return RecipeSearchResponseDTO.fromEntity(recipe, avgRating, reviewCount, liked);
        }).collect(Collectors.toList());
    }

    // 특정 레시피 조회 (조회수 증가)
    @Transactional
    public Recipe getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("레시피를 찾을 수 없습니다: " + id));
        recipe.setViewCount(recipe.getViewCount() + 1);
        // JPA 변경감지로 저장
        return recipe;
    }

    // 발행 레시피 생성 (초안 API와 구분)
    @Transactional
    public Recipe createRecipe(RecipeDTO dto, String username) {
        // 초안 생성은 /api/recipes/drafts 로 분리되어 있으므로 방지
        if (Boolean.TRUE.equals(dto.getIsDraft())) {
            throw new IllegalArgumentException("초안은 /api/recipes/drafts 엔드포인트를 사용하세요.");
        }
        log.debug("createRecipe 호출됨");
        log.debug("username = {}", username);
        log.debug("dto.title = {}", dto.getTitle());
        log.debug("dto.category = {}", dto.getCategory());
        log.debug("dto.difficulty = {}", dto.getDifficulty());
        log.debug("dto.tags = {}", dto.getTags());
        log.debug("dto.cookingTime = {}", dto.getCookingTime());
        log.debug("dto.servings = {}", dto.getServings());
        log.debug("dto.isPublic = {}", dto.getIsPublic());
        log.debug("dto.isDraft = {}", dto.getIsDraft());
        log.debug("dto.mainImageUrl = {}", dto.getMainImageUrl());
        log.debug("dto.videoUrl = {}", dto.getVideoUrl());
        log.debug("dto.ingredients = {}", dto.getIngredients());
        log.debug("dto.alternativeIngredients = {}", dto.getAlternativeIngredients());
        log.debug("dto.handlingMethods = {}", dto.getHandlingMethods());
        log.debug("dto.cookingSteps = {}", dto.getCookingSteps());

        UserEntity user = userRepository.findByUsername(username);

        // 발행용 엔티티
        Recipe recipe = dto.toEntity(); // dto는 래퍼 타입 + 기본값 포함

        recipe.setUser(user);
        recipe.setDraft(false);
        recipe.setPublic(Boolean.TRUE.equals(dto.getIsPublic()));

        pointService.addPoint(user, PointActionType.RECIPE_WRITE, 1, "레시피 작성 포인트 10점 적립");

        Recipe savedRecipe = recipeRepository.save(recipe);

        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            List<RecipeIngredient> ingList = dto.getIngredients().stream()
                    .map(riDto -> RecipeIngredient.builder()
                            .recipe(savedRecipe)
                            .ingredient(ingredientMasterRepository.findById(riDto.getId())
                                    .orElseThrow(() -> new IllegalArgumentException("재료 없음: " + riDto.getId())))
                            .quantity(riDto.getAmount())
                            .build())
                    .toList();

            // recipe에 세팅 + 영속화
            savedRecipe.setIngredients(ingList);
            recipeIngredientRepository.saveAll(ingList);
        }

        // 수정된 부분: 썸네일 자동 생성 로직
        // 조건 1 & 2: 썸네일이 없고 조리순서가 있을 때만 실행 (기존 조건문 그대로 유지)
        if ((savedRecipe.getMainImageUrl() == null || savedRecipe.getMainImageUrl().isBlank())
                && savedRecipe.getCookingSteps() != null
                && !savedRecipe.getCookingSteps().trim().isEmpty()) {
            try {
                // 프롬프트 생성은 기존 함수(buildPrompt)를 그대로 사용 (빠른 작업이므로 동기 처리)
                String prompt = buildPrompt(savedRecipe);

                // 기존 동기 호출(openAiService.generateThumbnail)을 지우고 비동기 서비스 호출로 대체!
                thumbnailAsyncService.generateAndSaveThumbnailAsync(savedRecipe.getRecipeId(), prompt);

            } catch (Exception e) {
                log.error("썸네일 비동기 호출 중 에러: {}", e.getMessage());
            }
        }

        return savedRecipe;
    }

    // 발행 레시피 수정 (null-세이프 업데이트)
    @Transactional
    public Recipe updateRecipe(Long id, RecipeDTO dto) {
        Recipe r = getRecipeById(id);

        if (dto.getTitle() != null) r.setTitle(dto.getTitle());
        if (dto.getCategory() != null) r.setCategory(RecipeCategory.valueOf(dto.getCategory()));
        if (dto.getIngredients() != null) {
            // 기존 재료 삭제 후 새로 교체
            List<RecipeIngredient> ingList = dto.getIngredients().stream()
                    .map(riDto -> RecipeIngredient.builder()
                            .recipe(r)
                            .ingredient(ingredientMasterRepository.findById(riDto.getId())
                                    .orElseThrow(() -> new IllegalArgumentException("재료 없음: " + riDto.getId())))
                            .quantity(riDto.getAmount())
                            .build())
                    .toList();
            r.getIngredients().clear();
            r.getIngredients().addAll(ingList);
        }

        if (dto.getAlternativeIngredients() != null) r.setAlternativeIngredients(dto.getAlternativeIngredients());
        if (dto.getHandlingMethods() != null) r.setHandlingMethods(dto.getHandlingMethods());
        if (dto.getCookingSteps() != null) r.setCookingSteps(dto.getCookingSteps());
        if (dto.getMainImageUrl() != null) r.setMainImageUrl(dto.getMainImageUrl());
        if (dto.getDifficulty() != null) r.setDifficulty(RecipeDifficulty.valueOf(dto.getDifficulty()));
        if (dto.getTags() != null) r.setTags(dto.getTags());
        if (dto.getCookingTime() != null) r.setCookingTime(dto.getCookingTime());
        if (dto.getServings() != null) r.setServings(dto.getServings());
        if (dto.getVideoUrl() != null) r.setVideoUrl(dto.getVideoUrl());
        if (dto.getRecipeType() != null) r.setRecipeType(RecipeType.valueOf(dto.getRecipeType()));
        if (dto.getIsPublic() != null) r.setPublic(dto.getIsPublic());
        // 드래프트 여부는 일반 수정에서 바꾸지 않음 (발행/초안 전환은 전용 API 사용)

        return r; // @Transactional 변경감지
    }

    // 레시피 삭제
    @Transactional
    public void deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("레시피를 찾을 수 없습니다: " + id));
        recipeRepository.delete(recipe);
    }

    // 레시피 검색 (공개 + 초안 제외)
    private UserEntity currentUserOrNull() {
        String username = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(a -> a.getName()).orElse(null);
        return (username != null && !"anonymousUser".equalsIgnoreCase(username))
                ? userRepository.findByUsername(username)
                : null;
    }

    public List<RecipeSearchResponseDTO> toSearchDtoList(List<Recipe> recipes) {
        UserEntity currentUser = currentUserOrNull();
        return recipes.stream().map(recipe -> {
            Double avgRatingWrapper = reviewRepository.findAvgRatingByRecipe(recipe.getRecipeId());
            double avgRating = avgRatingWrapper != null ? avgRatingWrapper : 0.0;
            int reviewCount = reviewRepository.countByRecipe(recipe);
            boolean liked = (currentUser != null) && likeRecipeRepository.existsByUserAndRecipe(currentUser, recipe);
            return RecipeSearchResponseDTO.fromEntity(recipe, avgRating, reviewCount, liked);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<RecipeSearchResponseDTO> searchRecipesByTitle(String title) {
        List<Recipe> recipes = (title == null || title.trim().isEmpty())
                ? recipeRepository.findByIsPublicTrueAndIsDraftFalse()
                : recipeRepository.findByTitleContainingIgnoreCaseAndIsPublicTrueAndIsDraftFalse(title);
        return toSearchDtoList(recipes);  // 공통 매퍼 재사용
    }

    // 메인 - 냉장고 재료 추천 레시피(제목 키워드)
    @Transactional(readOnly = true)
    public List<RecipeSearchResponseDTO> getRecommendedRecipesByTitleKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<Recipe> recipes = recipeRepository.findByIsPublicTrue();

        List<Recipe> filtered = recipes.stream()
                .filter(recipe -> keywords.stream().anyMatch(keyword -> recipe.getTitle().contains(keyword)))
                .sorted(Comparator.comparingInt(Recipe::getViewCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return filtered.stream()
                .map(recipe -> RecipeSearchResponseDTO.fromEntity(recipe, 0.0, recipe.getLikes(), false))
                .collect(Collectors.toList());
    }

    // 메인 - 냉장고 재료 추천 레시피 그룹
    @Transactional(readOnly = true)
    public List<IngredientRecipeGroup> getGroupedRecommendedRecipesByTitle(List<String> keywords) {
        List<Recipe> allRecipes = recipeRepository.findByIsPublicTrue();

        return keywords.stream()
                .map(keyword -> {
                    List<Recipe> matched = allRecipes.stream()
                            .filter(recipe -> recipe.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                            .sorted(Comparator.comparingInt(Recipe::getViewCount).reversed())
                            .limit(2)
                            .collect(Collectors.toList());

                    List<RecipeSearchResponseDTO> dtos = matched.stream()
                            .map(recipe -> RecipeSearchResponseDTO.fromEntity(recipe, 0.0, recipe.getLikes(), false))
                            .collect(Collectors.toList());

                    return new IngredientRecipeGroup(keyword, dtos);
                })
                .collect(Collectors.toList());
    }

    // 예상 사용 재료 (NPE 가드)
    // 예상 사용 재료 (냉장고 매칭)
    @Transactional(readOnly = true)
    public List<ExpectedIngredientDTO> getExpectedIngredients(Long recipeId, UserEntity user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("레시피를 찾을 수 없습니다."));

        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return Collections.emptyList();
        }

        List<ExpectedIngredientDTO> result = new ArrayList<>();

        for (RecipeIngredient ri : recipe.getIngredients()) {
            String name = ri.getIngredient().getNameKo();
            Double amount = ri.getQuantity();
            String unitDetail = ri.getIngredient().getDefaultUnit().getName();

            List<Fridge> matched = fridgeRepository.findAllByUserAndIngredientNameOrderByCreatedAtAsc(user, name);

            if (!matched.isEmpty()) {
                double totalQuantity = matched.stream().mapToDouble(Fridge::getQuantity).sum();
                String fridgeDate = matched.stream()
                        .map(Fridge::getFridgeDate)
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .findFirst()
                        .orElse("날짜 없음");
                String dateOption = matched.get(0).getDateOption();

                result.add(new ExpectedIngredientDTO(
                        name,
                        String.valueOf(amount),
                        String.valueOf(totalQuantity),
                        unitDetail,
                        fridgeDate,
                        dateOption
                ));
            }
        }

        return result;
    }


    // 관리자 삭제
    @Transactional
    public void deleteRecipeByAdmin(Long recipeId, String adminUsername, String reason) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피가 존재하지 않습니다."));

        likeRecipeRepository.deleteAllByRecipe(recipe);
        reviewRepository.deleteAllByRecipe(recipe);
        recommendRecipeRepository.deleteAllByRecipe(recipe);
        recipeRepository.delete(recipe);

        adminLogService.logAdminAction(
                adminUsername,
                "DELETE_RECIPE",
                "RECIPE",
                recipeId,
                reason
        );
    }

    // 썸네일 생성용 프롬프트
    private String buildPrompt(Recipe recipe) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(recipe.getCategory()).append(" 요리인 ").append(recipe.getTitle()).append("의 음식 사진입니다. ");

        if (recipe.getTags() != null && !recipe.getTags().isBlank()) {
            prompt.append("이 요리는 ").append(recipe.getTags()).append(" 느낌을 줍니다. ");
        }

        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            prompt.append("주요 재료는 ");
            prompt.append(recipe.getIngredients().stream()
                            .map(ri -> ri.getIngredient().getNameKo())
                            .filter(Objects::nonNull)
                            .limit(10)
                            .collect(Collectors.joining(", ")))
                    .append("입니다. ");
        }

        try {
            Gson gson = new Gson();
            Type stepType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> steps = gson.fromJson(recipe.getCookingSteps(), stepType);
            if (steps != null && !steps.isEmpty()) {
                prompt.append("조리 순서는 다음과 같습니다: ");
                prompt.append(steps.stream()
                        .map(step -> String.valueOf(step.get("description")))
                        .collect(Collectors.joining(", "))).append(". ");
            }
        } catch (Exception ignored) { }

        prompt.append("이 레시피의 썸네일에 사용할 하얀색 배경에 음식 사진을 생성해주세요.");
        return prompt.toString();
    }

    // 레시피 탭 - 레시피 이거 어때요?
    @Transactional(readOnly = true)
    public List<RecipeSearchResponseDTO> suggestByType(String type) {
        String regex = switch (type) {
            case "lateNightMeal" -> "(곱창|닭|치킨|닭발|피자|라면|떡볶이)";
            case "rainsDay" -> "(수제비|칼국수|감자탕|전)";
            case "cool" -> "(초계국수|열무국수|냉면|비빔냉면|모밀)";
            case "heat" -> "(삼계탕|닭죽|전골)";
            case "vegan" -> "(비건|채식)";
            case "superSimple" -> "(계란찜|볶음밥|비빔밥|미역국)";
            default -> throw new IllegalArgumentException("invalid type: " + type);
        };

        String username = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(a -> a.getName())
                .filter(n -> !"anonymousUser".equalsIgnoreCase(n))
                .orElse(null);
        UserEntity currentUser = (username != null) ? userRepository.findByUsername(username) : null;

        List<Recipe> pick = recipeRepository.findRandomPublicByRegex(regex, SUGGEST_LIMIT);

        return pick.stream().map(recipe -> {
            Double avgRatingWrapper = reviewRepository.findAvgRatingByRecipe(recipe.getRecipeId());
            double avgRating = (avgRatingWrapper != null) ? avgRatingWrapper : 0.0;
            int reviewCount = reviewRepository.countByRecipe(recipe);
            boolean liked = (currentUser != null) && likeRecipeRepository.existsByUserAndRecipe(currentUser, recipe);
            return RecipeSearchResponseDTO.fromEntity(recipe, avgRating, reviewCount, liked);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<RecipeDTO> findRecipesByTitlesContaining(List<String> keywords) {
        // 공개 레시피만 가져온 뒤 제목 포함 여부로 필터
        List<Recipe> allPublic = recipeRepository.findByIsPublicTrue();

        List<Recipe> filtered = allPublic.stream()
                .filter(recipe -> {
                    String title = Optional.ofNullable(recipe.getTitle()).orElse("").toLowerCase();
                    return keywords.stream().anyMatch(k -> title.contains(k.toLowerCase()));
                })
                .collect(Collectors.toList());

        return filtered.stream()
                .map(RecipeDTO::fromEntity)
                .collect(Collectors.toList());
    }

}
