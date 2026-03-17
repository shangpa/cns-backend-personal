package com.example.cns.fridge.recommend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/fridge/recommend")
@RequiredArgsConstructor
public class FridgeRecommendController {

    private final FridgeRecommendService fridgeRecommendService;

    @PostMapping
    public ResponseEntity<List<RecipeRecommendResponseDTO>> recommendRecipes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RecipeRecommendRequestDTO requestDTO
    ) {
        // 전달된 재료 ID 확인 로그
        log.debug("[FridgeRecommendController] 요청 유저: {}", (userDetails != null ? userDetails.getUsername() : "비로그인"));
        log.debug("[FridgeRecommendController] 전달된 ingredientIds: {}", requestDTO.getSelectedIngredientIds());

        List<Long> ids = requestDTO.getSelectedIngredientIds();
        if (ids != null && !ids.isEmpty()) {
            List<RecipeRecommendResponseDTO> result = fridgeRecommendService.recommendRecipes(ids);
            log.debug("추천된 레시피 개수: {}", result.size());
            for (RecipeRecommendResponseDTO r : result) {
                log.debug(" - {} (id={})", r.getTitle(), r.getRecipeId());
            }
            return ResponseEntity.ok(result);
        }
        log.warn("전달된 재료 ID가 없음!");
        return ResponseEntity.ok(List.of());
    }
}