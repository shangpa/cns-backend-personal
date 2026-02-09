package com.example.springjwt.fridge.recommend;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        // ✅ 전달된 재료 ID 확인 로그
        System.out.println("========== [FridgeRecommendController] ==========");
        System.out.println("요청 유저: " + (userDetails != null ? userDetails.getUsername() : "비로그인"));
        System.out.println("전달된 ingredientIds: " + requestDTO.getSelectedIngredientIds());
        System.out.println("===============================================");

        List<Long> ids = requestDTO.getSelectedIngredientIds();
        if (ids != null && !ids.isEmpty()) {
            List<RecipeRecommendResponseDTO> result = fridgeRecommendService.recommendRecipes(ids);
            System.out.println("추천된 레시피 개수: " + result.size());
            result.forEach(r -> System.out.println(" - " + r.getTitle() + " (id=" + r.getRecipeId() + ")"));
            return ResponseEntity.ok(result);
        }
        System.out.println("⚠️ 전달된 재료 ID가 없음!");
        return ResponseEntity.ok(List.of());
    }
}