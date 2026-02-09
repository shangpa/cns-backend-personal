package com.example.springjwt.mypage;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.recipe.Recipe;
import com.example.springjwt.recipe.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recipes")
public class RecommendRecipeController {

    private final UserRepository userRepository;
    private final RecommendRecipeRepository recommendRecipeRepository;
    private final RecipeRepository recipeRepository;

    //추천 버튼 토글
    @PostMapping("/{recipeId}/recommend-toggle")
    public ResponseEntity<String> toggleRecommendRecipe(
            @PathVariable Long recipeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        UserEntity user = userRepository.findByUsername(username);
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("레시피 없음"));

        Optional<RecommendRecipe> existing = recommendRecipeRepository.findByUserAndRecipe(user, recipe);

        if (existing.isPresent()) {
            recommendRecipeRepository.delete(existing.get());
            recipe.setRecommends(recipe.getRecommends() - 1);
            recipeRepository.save(recipe);
            return ResponseEntity.ok("추천 취소됨");
        }

        RecommendRecipe recommend = new RecommendRecipe();
        recommend.setUser(user);
        recommend.setRecipe(recipe);
        recommendRecipeRepository.save(recommend);

        recipe.setRecommends(recipe.getRecommends() + 1);

        /*// 포인트 로직 (예시: 추천 5개마다 포인트 지급)
        int newStep = recipe.getRecommends() / 5;
        int prevStep = recipe.getRecommendPointStep(); // 필드 추가 필요

        if (newStep > prevStep) {
            int diff = newStep - prevStep;
            pointService.addPoint(
                    recipe.getUser(),
                    PointActionType.RECIPE_RECOMMEND_RECEIVED,
                    diff * 10,
                    "레시피 추천 " + recipe.getRecommends() + "개 돌파"
            );
            recipe.setRecommendPointStep(newStep);
        }*/

        recipeRepository.save(recipe);

        return ResponseEntity.ok("추천 추가됨");
    }
    //추천 여부확인
    @GetMapping("/{recipeId}/recommended")
    public ResponseEntity<Boolean> isRecipeRecommended(
            @PathVariable Long recipeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        UserEntity user = userRepository.findByUsername(username);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("레시피 없음"));

        boolean recommended = recommendRecipeRepository.findByUserAndRecipe(user, recipe).isPresent();
        return ResponseEntity.ok(recommended);
    }


}
