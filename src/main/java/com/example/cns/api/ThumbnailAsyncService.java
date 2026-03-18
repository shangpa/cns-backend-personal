package com.example.cns.api;

import com.example.cns.api.OpenAiService;
import com.example.cns.recipe.Recipe;
import com.example.cns.recipe.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailAsyncService {

    private final RecipeRepository recipeRepository;
    private final OpenAiService openAiService;

    // 핵심: 이 메서드만 백그라운드 스레드에서 돌아갑니다.
    @Async
    @Transactional
    public void generateAndSaveThumbnailAsync(Long recipeId, String prompt) {
        try {
            // 1. 시간이 오래 걸리는 외부 API 호출 (비동기)
            String imageUrl = openAiService.generateThumbnail(prompt);

            // 2. 이미지 생성이 완료되면 기존 레시피를 찾아서 업데이트
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다."));

            recipe.setMainImageUrl(imageUrl);
            recipeRepository.save(recipe); // 변경 감지(Dirty Checking) 또는 명시적 save

        } catch (Exception e) {
            // 백그라운드에서 터진 에러이므로 로그로 꼭 남겨줍니다.
            log.error("비동기 썸네일 생성 중 에러 발생: {}", e.getMessage());
        }
    }
}