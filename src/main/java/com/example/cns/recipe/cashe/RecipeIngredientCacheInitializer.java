package com.example.cns.recipe.cashe;

import com.example.cns.ingredient.IngredientMasterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.example.cns.ingredient.IngredientMaster;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RecipeIngredientCacheInitializer {

    private final IngredientMasterRepository ingredientMasterRepository;
    private final IngredientNameCache ingredientNameCache;

    @PostConstruct
    public void init() {
        // 마스터 테이블에서 모든 재료명 가져오기
        List<String> allIngredientNames = ingredientMasterRepository.findAll().stream()
                .map(IngredientMaster::getNameKo)
                .toList();

        ingredientNameCache.initialize(allIngredientNames);
        System.out.println("✅ 재료 이름 캐시 초기화 완료: 총 " + allIngredientNames.size() + "개");
    }
}

