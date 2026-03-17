package com.example.cns.recipe;

import com.example.cns.dto.CustomUserDetails;
import com.example.cns.search.SearchKeywordService;
import com.example.cns.search.SeasonalRecipeDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import com.example.cns.search.RecipeSearchService;
import com.example.cns.search.SortKey;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeDraftService recipeDraftService;
    private final SearchKeywordService searchKeywordService;
    private final RecipeRepository recipeRepository;
    private final RecipeSearchService recipeSearchService;
    private final RecommendService recommendService;

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
            @Valid @RequestBody RecipeDTO recipeDTO,
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
        RecipeDTO draft = recipeDraftService.getMyDraftById(recipeId, userDetails.getUserEntity());
        return ResponseEntity.ok(draft);
    }

    // 임시저장 생성
    @PostMapping("/drafts")
    public ResponseEntity<RecipeResponseDTO> createDraft(
            @RequestBody RecipeDTO dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long id = recipeDraftService.createDraftTransactional(dto, user.getUserEntity());
        return ResponseEntity.ok(new RecipeResponseDTO(true, "임시저장 생성", id));
    }

    // 임시저장 수정(작성 중 계속 저장)
    @PutMapping("/drafts/{id}")
    public ResponseEntity<RecipeDTO> updateDraft(
            @PathVariable Long id,
            @RequestBody RecipeDTO dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Recipe saved = recipeDraftService.updateDraft(id, dto, user.getUserEntity().getId());
        return ResponseEntity.ok(RecipeDTO.fromEntity(saved));
    }

    // 임시저장 삭제
    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<Void> deleteMyDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        recipeDraftService.deleteDraft(id, user.getUserEntity().getId());
        return ResponseEntity.noContent().build();
    }

    // 임시저장을 발행(공개/비공개)으로 전환
    @PostMapping("/{id}/publish")
    public ResponseEntity<RecipeDTO> publish(
            @PathVariable Long id,
            @RequestBody PublishRequest req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Recipe saved = recipeDraftService.publishDraft(id, Boolean.TRUE.equals(req.getIsPublic()), user.getUserEntity().getId());
        return ResponseEntity.ok(RecipeDTO.fromEntity(saved));
    }

    // 발행 요청 바디
    @Getter
    @Setter
    public static class PublishRequest {
        private Boolean isPublic; // true/false
    }
}
