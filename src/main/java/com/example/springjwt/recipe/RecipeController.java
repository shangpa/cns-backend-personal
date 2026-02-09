package com.example.springjwt.recipe;

import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.ingredient.IngredientMasterRepository;
import com.example.springjwt.recipe.expected.ExpectedIngredientDTO;
import com.example.springjwt.recipeingredient.RecipeIngredient;
import com.example.springjwt.recipeingredient.RecipeIngredientRepository;
import com.example.springjwt.search.SearchKeywordService;
import com.example.springjwt.search.SeasonalRecipeDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import com.example.springjwt.search.RecipeSearchService;
import com.example.springjwt.search.SortKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final SearchKeywordService searchKeywordService;
    private final RecipeRepository recipeRepository;
    private final RecipeSearchService recipeSearchService;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final RecommendService recommendService;
    private final RecipeIngredientRepository recipeIngredientRepository;

    // 레시피 전체 조회
    @GetMapping
    public ResponseEntity<List<RecipeDTO>> getAllRecipes() {
        List<RecipeDTO> recipes = recipeService.getAllRecipes()
                .stream()
                .map(RecipeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recipes);
    }

    // 공개 레시피 조회 (정렬 옵션)
    @GetMapping("/public")
    public List<RecipeSearchResponseDTO> getPublicRecipes(
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return recipeService.getAllPublicRecipes(sort);
    }

    // 특정 레시피 조회 (+조회수 증가)
    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> getRecipeById(@PathVariable Long id) {
        Recipe recipe = recipeService.getRecipeById(id);
        return ResponseEntity.ok(RecipeDTO.fromEntity(recipe));
    }

    // 레시피 생성(발행)
    @PostMapping
    public ResponseEntity<RecipeResponseDTO> createRecipe(
            @RequestBody RecipeDTO recipeDTO,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Recipe recipe = recipeService.createRecipe(recipeDTO, userDetails.getUsername());
        RecipeResponseDTO response = new RecipeResponseDTO(
                true,
                "레시피가 성공적으로 생성되었습니다.",
                recipe.getRecipeId()
        );
        return ResponseEntity.ok(response);
    }

    // 레시피 수정(발행 상태)
    @PutMapping("/{id}")
    public ResponseEntity<RecipeDTO> updateRecipe(
            @PathVariable Long id,
            @RequestBody RecipeDTO recipeDTO
    ) {
        Recipe updatedRecipe = recipeService.updateRecipe(id, recipeDTO);
        return ResponseEntity.ok(RecipeDTO.fromEntity(updatedRecipe));
    }

    // 레시피 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    // 프론트가 호출하는 검색 (리스트)
    @GetMapping("/search")
    public List<RecipeSearchResponseDTO> searchRecipes(
            @RequestParam String title,
            @RequestParam(required = false) String category,   // "koreaFood" 등
            @RequestParam(required = false) String sort        // viewCount|likes|latest|shortTime|longTime
    ) {
        SortKey key = parseSort(sort);
        Page<Recipe> page = recipeSearchService.search(title, category, key, 0, 200);
        return recipeService.toSearchDtoList(page.getContent()); // 여기서 DTO 변환
    }

    // ✅ 페이징 버전 (선택)
    @GetMapping("/search/page")
    public Page<Recipe> searchRecipesPaged(
            @RequestParam String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (title != null && !title.isBlank()) {
            searchKeywordService.saveKeyword(title);
        }
        SortKey key = parseSort(sort);
        return recipeSearchService.search(title, category, key, page, size);
    }

    private SortKey parseSort(String sort) {
        if (sort == null || sort.isBlank()) return SortKey.latest;
        try { return SortKey.valueOf(sort); } catch (IllegalArgumentException e) { return SortKey.latest; }
    }

    // 메인 - 냉장고 재료 추천 레시피
    @GetMapping("/recommend-by-title")
    public ResponseEntity<List<RecipeSearchResponseDTO>> recommendByTitle(
            @RequestParam List<String> ingredients
    ) {
        List<RecipeSearchResponseDTO> recommended =
                recipeService.getRecommendedRecipesByTitleKeywords(ingredients);
        return ResponseEntity.ok(recommended);
    }

    // 메인 - 냉장고 재료 추천 레시피 그룹
    @PostMapping("/recommend-grouped")
    public ResponseEntity<List<IngredientRecipeGroup>> recommendGroupedByTitle(
            @RequestBody List<String> ingredients
    ) {
        return ResponseEntity.ok(recommendService.recommendGrouped(ingredients));
    }

    // 메인 - 레시피 조회 TOP6
    @GetMapping("/top/view")
    public ResponseEntity<List<RecipeSearchResponseDTO>> getTopViewedRecipes() {
        List<Recipe> top = recipeRepository.findTop6ByIsPublicTrueOrderByViewCountDesc();
        List<RecipeSearchResponseDTO> result = top.stream()
                .map(r -> RecipeSearchResponseDTO.fromEntity(r, 0.0, 0, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 제철 음식 추천 (제목 기준)
    @GetMapping("/seasonal")
    public List<SeasonalRecipeDto> getSeasonalRecipes() {
        List<String> seasonalTitles = List.of("삼계탕", "초계국수", "콩국수", "물회", "오이냉국");
        return recipeRepository.findSeasonalByExactTitles(seasonalTitles); // 레포지토리 프로젝션 쿼리 사용 권장
    }

    // 레시피 탭 - 레시피 이거 어때요?
    @GetMapping("/suggest")
    public ResponseEntity<List<RecipeSearchResponseDTO>> suggest(@RequestParam String type) {
        List<RecipeSearchResponseDTO> list = recipeService.suggestByType(type);
        return ResponseEntity.ok(list);
    }

    // ===================== 임시저장(Draft) API =====================

    // 내 임시저장 레시피 리스트
    @GetMapping("/drafts")
    public ResponseEntity<List<RecipeDTO>> getMyDrafts(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int uid = userDetails.getUserEntity().getId();
        List<Recipe> drafts = recipeRepository.findByUserIdAndIsDraftTrueOrderByCreatedAtDesc(uid);
        List<RecipeDTO> result = drafts.stream()
                .map(RecipeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 임시저장 단건 조회(소유자 검증)
    @GetMapping("/drafts/{recipeId}")
    public ResponseEntity<RecipeDTO> getMyDraftById(
            @PathVariable Long recipeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RecipeDTO draft = recipeService.getMyDraftById(recipeId, userDetails.getUserEntity());
        return ResponseEntity.ok(draft);
    }

    // 임시저장 생성
    @PostMapping("/drafts")
    public ResponseEntity<RecipeResponseDTO> createDraft(
            @RequestBody RecipeDTO dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long id = recipeService.createDraftTransactional(dto, user.getUserEntity());
        return ResponseEntity.ok(new RecipeResponseDTO(true, "임시저장 생성", id));
    }

    // 임시저장 수정(작성 중 계속 저장)
    @PutMapping("/drafts/{id}")
    public ResponseEntity<RecipeDTO> updateDraft(
            @PathVariable Long id,
            @RequestBody RecipeDTO dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Recipe draft = recipeRepository
                .findByRecipeIdAndUserIdAndIsDraftTrue(id, user.getUserEntity().getId())
                .orElseThrow(() -> new RuntimeException("임시저장이 없거나 권한 없음."));

        // 제목
        if (StringUtils.hasText(dto.getTitle())) draft.setTitle(dto.getTitle());

        // 카테고리/난이도/레시피타입: 빈문자 무시
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

        // 재료 교체
        if (dto.getIngredients() != null) {
            List<RecipeIngredient> ingList = dto.getIngredients().stream()
                    .filter(riDto -> riDto.getId() != null)
                    .map(riDto -> RecipeIngredient.builder()
                            .ingredient(ingredientMasterRepository.findById(riDto.getId())
                                    .orElseThrow(() -> new IllegalArgumentException("재료 없음: " + riDto.getId())))
                            .quantity(riDto.getAmount() != null ? riDto.getAmount() : 1.0)
                            .build())
                    .toList();

            draft.getIngredients().clear();        // orphanRemoval 로 기존 것 삭제
            for (RecipeIngredient ri : ingList) {
                ri.setRecipe(draft);               // 역방향 세팅
                draft.getIngredients().add(ri);
            }
        }

        // 지우기 허용 문자열들: null이면 미변경, ""이면 비우기
        if (dto.getAlternativeIngredients() != null) draft.setAlternativeIngredients(dto.getAlternativeIngredients());
        if (dto.getHandlingMethods()        != null) draft.setHandlingMethods(dto.getHandlingMethods());
        if (dto.getCookingSteps()           != null) draft.setCookingSteps(dto.getCookingSteps());
        if (dto.getMainImageUrl()           != null) draft.setMainImageUrl(dto.getMainImageUrl());
        if (dto.getTags()                   != null) draft.setTags(dto.getTags());
        if (dto.getVideoUrl()               != null) draft.setVideoUrl(dto.getVideoUrl());

        // 숫자형
        if (dto.getCookingTime() != null) draft.setCookingTime(dto.getCookingTime());
        if (dto.getServings()    != null) draft.setServings(dto.getServings());

        // 강제 초안 상태 유지
        draft.setDraft(true);
        draft.setPublic(false);

        Recipe saved = recipeRepository.save(draft);
        return ResponseEntity.ok(RecipeDTO.fromEntity(saved));
    }

    // 임시저장 삭제
    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<Void> deleteMyDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Recipe draft = recipeRepository
                .findByRecipeIdAndUserIdAndIsDraftTrue(id, user.getUserEntity().getId())
                .orElseThrow(() -> new RuntimeException("임시저장이 없거나 권한 없음."));
        recipeRepository.delete(draft);
        return ResponseEntity.noContent().build();
    }

    // 임시저장을 발행(공개/비공개)으로 전환
    @PostMapping("/{id}/publish")
    public ResponseEntity<RecipeDTO> publish(
            @PathVariable Long id,
            @RequestBody PublishRequest req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Recipe draft = recipeRepository
                .findByRecipeIdAndUserIdAndIsDraftTrue(id, user.getUserEntity().getId())
                .orElseThrow(() -> new RuntimeException("임시저장이 없거나 권한 없음."));

        // 필수 검증(필요 시 추가)
        if (draft.getTitle() == null || draft.getCategory() == null) {
            throw new IllegalArgumentException("제목/카테고리는 필수입니다.");
        }

        draft.setDraft(false); // 발행
        draft.setPublic(Boolean.TRUE.equals(req.getIsPublic()));

        Recipe saved = recipeRepository.save(draft);
        return ResponseEntity.ok(RecipeDTO.fromEntity(saved));
    }

    // 발행 요청 바디
    @Getter
    @Setter
    public static class PublishRequest {
        private Boolean isPublic; // true/false
    }
}
