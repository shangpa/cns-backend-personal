package com.example.springjwt.recipe;

import com.example.springjwt.ingredient.IngredientMaster;
import com.example.springjwt.ingredient.IngredientMasterRepository;
import com.example.springjwt.recipeingredient.RecipeIngredient;
import com.example.springjwt.recipeingredient.RecipeIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final IngredientMasterRepository ingredientRepo;
    private final RecipeIngredientRepository riRepo;

    /**
     * 입력받은 재료명 리스트를 재료별로 레시피 그룹으로 반환
     * 프런트: POST /api/recipes/recommend-grouped 로 List<String> 보냄
     */
    public List<IngredientRecipeGroup> recommendGrouped(List<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) return List.of();

        // 1) 재료명 정규화
        List<String> names = rawNames.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (names.isEmpty()) return List.of();

        List<IngredientRecipeGroup> result = new ArrayList<>();

        for (String name : names) {
            // 2) 재료 마스터 찾기 (한글명, 대소문자 무시)
            Optional<IngredientMaster> opt = ingredientRepo.findByNameKoIgnoreCase(name);
            if (opt.isEmpty()) {
                result.add(new IngredientRecipeGroup(name, List.of()));
                continue;
            }
            IngredientMaster master = opt.get();

            // 3) 해당 재료가 들어간 레시피들 조회
            List<RecipeIngredient> ris = riRepo.findByIngredient_Id(master.getId());

            // 4) 레시피 중복 제거 + 가볍게 정렬(조회수 내림차순 등) + 상위 n개 선택
            List<Recipe> recipes = ris.stream()
                    .map(RecipeIngredient::getRecipe)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted((a, b) -> Integer.compare(
                            safeInt(b.getViewCount()), safeInt(a.getViewCount())))
                    .limit(10) // 넉넉히 뽑고 프런트에서 2개만 쓰더라도 OK
                    .toList();

            // 5) DTO 매핑 (당신 프로젝트의 fromEntity 사용)
            //    평균/리뷰/좋아요 여부가 필요하면 실제 서비스/리포지토리로 계산해 넣으세요.
            List<RecipeSearchResponseDTO> dtoList = recipes.stream()
                    .map(r -> RecipeSearchResponseDTO.fromEntity(
                            r,
                            /* avgRating  */ 0.0,
                            /* reviewCnt  */ 0,
                            /* liked?     */ false
                    ))
                    .collect(Collectors.toList());

            result.add(new IngredientRecipeGroup(master.getNameKo(), dtoList));
        }

        return result;
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }
}
