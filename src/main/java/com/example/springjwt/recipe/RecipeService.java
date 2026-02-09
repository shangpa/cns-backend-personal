package com.example.springjwt.recipe;

import com.example.springjwt.admin.dto.BoardMonthlyStatsDTO;
import com.example.springjwt.admin.dto.RecipeMonthlyStatsDTO;
import com.example.springjwt.admin.dto.RecipeStatDTO;
import com.example.springjwt.admin.enums.StatType;
import com.example.springjwt.admin.log.AdminLogService;
import com.example.springjwt.api.OpenAiService;
import com.example.springjwt.api.vision.IngredientParser;
import com.example.springjwt.fridge.Fridge;
import com.example.springjwt.fridge.FridgeRepository;
import com.example.springjwt.ingredient.IngredientMasterRepository;
import com.example.springjwt.mypage.LikeRecipeRepository;
import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.mypage.RecommendRecipeRepository;
import com.example.springjwt.point.PointActionType;
import com.example.springjwt.point.PointService;
import com.example.springjwt.recipe.cashe.IngredientNameCache;
import com.example.springjwt.recipe.expected.ExpectedIngredientDTO;
import com.example.springjwt.recipeingredient.RecipeIngredient;
import com.example.springjwt.recipeingredient.RecipeIngredientRepository;
import com.example.springjwt.review.Recipe.ReviewRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    // 전체 레시피 조회
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    // 공개된 레시피만 정렬해서 가져오기 (비로그인 가드 포함)
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
        System.out.println("📥 createRecipe 호출됨");
        System.out.println("username = " + username);
        System.out.println("dto.title = " + dto.getTitle());
        System.out.println("dto.category = " + dto.getCategory());
        System.out.println("dto.difficulty = " + dto.getDifficulty());
        System.out.println("dto.tags = " + dto.getTags());
        System.out.println("dto.cookingTime = " + dto.getCookingTime());
        System.out.println("dto.servings = " + dto.getServings());
        System.out.println("dto.isPublic = " + dto.getIsPublic());
        System.out.println("dto.isDraft = " + dto.getIsDraft());
        System.out.println("dto.mainImageUrl = " + dto.getMainImageUrl());
        System.out.println("dto.videoUrl = " + dto.getVideoUrl());
        System.out.println("dto.ingredients = " + dto.getIngredients());
        System.out.println("dto.alternativeIngredients = " + dto.getAlternativeIngredients());
        System.out.println("dto.handlingMethods = " + dto.getHandlingMethods());
        System.out.println("dto.cookingSteps = " + dto.getCookingSteps());

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

        // 썸네일 자동 생성 (메인이미지 없고, 조리순서가 있을 때)
        if ((savedRecipe.getMainImageUrl() == null || savedRecipe.getMainImageUrl().isBlank())
                && savedRecipe.getCookingSteps() != null
                && !savedRecipe.getCookingSteps().trim().isEmpty()) {
            try {
                String prompt = buildPrompt(savedRecipe);
                String imageUrl = openAiService.generateThumbnail(prompt);
                savedRecipe.setMainImageUrl(imageUrl);
                // 변경감지로 저장
            } catch (Exception e) {
                // 로깅만
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

    public List<RecipeSearchResponseDTO> searchRecipesByTitle(String title) {
        List<Recipe> recipes = (title == null || title.trim().isEmpty())
                ? recipeRepository.findByIsPublicTrueAndIsDraftFalse()
                : recipeRepository.findByTitleContainingIgnoreCaseAndIsPublicTrueAndIsDraftFalse(title);
        return toSearchDtoList(recipes);  // 공통 매퍼 재사용
    }

    //관리자 - 카테고리별 통계
    public List<RecipeStatDTO> getCategoryStats() {
        List<Object[]> raw = recipeRepository.countByCategory();

        Map<RecipeCategory, Long> map = raw.stream()
                .collect(Collectors.toMap(
                        obj -> (RecipeCategory) obj[0],
                        obj -> (Long) obj[1]
                ));

        List<RecipeStatDTO> result = new ArrayList<>();
        for (RecipeCategory category : RecipeCategory.values()) {
            long count = map.getOrDefault(category, 0L);
            result.add(new RecipeStatDTO(category.name(), count));
        }

        return result;
    }

    public List<RecipeStatDTO> getMonthlyCategoryStatsByName(String category) {
        try {
            RecipeCategory enumCategory = RecipeCategory.valueOf(category);
            return recipeRepository.countMonthlyBySpecificCategory(enumCategory).stream()
                    .map(obj -> new RecipeStatDTO(obj[0].toString(), (Long) obj[1]))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
        }
    }

    // 메인 - 냉장고 재료 추천 레시피(제목 키워드)
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


    // 대시보드/통계
    public List<RecipeMonthlyStatsDTO> getRecentFourMonthsStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fourMonthsAgo = now.minusMonths(3)
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return recipeRepository.findRecentRecipeCounts(fourMonthsAgo);
    }

    public List<BoardMonthlyStatsDTO> countRecipeMonthly(LocalDateTime startDate) {
        List<Object[]> raw = recipeRepository.countRecipeMonthlyRaw(startDate);
        return raw.stream()
                .map(row -> new BoardMonthlyStatsDTO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public List<BoardMonthlyStatsDTO> sumRecipeViewsMonthly(LocalDateTime startDate) {
        List<Object[]> raw = recipeRepository.sumRecipeViewsMonthlyRaw(startDate);
        return raw.stream()
                .map(row -> new BoardMonthlyStatsDTO((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public List<RecipeStatDTO> getRecipeStats(StatType type, LocalDate startDate, LocalDate endDate, Integer year, Integer month) {
        if (type == StatType.YEARLY && year != null) {
            return recipeRepository.countByYear(year).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "월", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.MONTHLY && year != null && month != null) {
            return recipeRepository.countByMonth(year, month).stream()
                    .map(obj -> new RecipeStatDTO(obj[0] + "일", (Long) obj[1]))
                    .collect(Collectors.toList());

        } else if (type == StatType.DAILY && startDate != null && endDate != null) {
            return recipeRepository.countByDateRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59)).stream()
                    .map(obj -> new RecipeStatDTO(obj[0].toString(), (Long) obj[1]))
                    .collect(Collectors.toList());
        }

        throw new IllegalArgumentException("유효하지 않은 파라미터입니다.");
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

    // 내 초안 단건 조회 (컨트롤러에서 사용)
    @Transactional(readOnly = true)
    public RecipeDTO getMyDraftById(Long recipeId, UserEntity user) {
        Recipe recipe = recipeRepository.findDraftWithIngredients(recipeId, user.getId())
                .orElseThrow(() -> new RuntimeException("임시저장 레시피를 찾을 수 없습니다."));
        return RecipeDTO.fromEntity(recipe);
    }

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

    // 임시저장
    @Transactional
    public Long createDraftTransactional(RecipeDTO dto, UserEntity user) {
        Recipe entity = dto.toEntityDraftSafe();
        if (entity.getRecipeType() == null) entity.setRecipeType(RecipeType.IMAGE);
        entity.setUser(user);
        entity.setDraft(true);
        entity.setPublic(false);
        entity.setCreatedAt(LocalDateTime.now());

        recipeRepository.save(entity); // 부모 영속화

        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            List<RecipeIngredient> ingList = dto.getIngredients().stream()
                    .map(riDto -> {
                        if (riDto.getId() == null && (riDto.getName() == null || riDto.getName().isBlank())) return null;

                        var master = (riDto.getId() != null)
                                ? ingredientMasterRepository.findById(riDto.getId()).orElse(null)
                                : ingredientMasterRepository.findByNameKoIgnoreCase(riDto.getName()).orElse(null);
                        if (master == null) return null;

                        Double qty = riDto.getAmount() != null ? riDto.getAmount() : 1.0; // 기본값 1.0
                        return RecipeIngredient.builder()
                                .ingredient(master)
                                .quantity(qty)
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!ingList.isEmpty()) {
                entity.getIngredients().clear();     // orphanRemoval 로 기존 것 제거
                for (RecipeIngredient ri : ingList) {
                    ri.setRecipe(entity);            // 🔴 역방향 세팅 필수
                    entity.getIngredients().add(ri); // 부모 컬렉션에 추가
                }
                recipeRepository.save(entity);       // 병합(변경감지로도 됨, 호출해도 무방)
            }
        }

        return entity.getRecipeId();
    }
}
