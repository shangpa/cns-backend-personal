package com.example.cns.api.vision;

import com.example.cns.User.UserEntity;
import com.example.cns.api.GoogleTranslateService;
import com.example.cns.api.IngredientDetectResponse;
import com.example.cns.fridge.Fridge;
import com.example.cns.fridge.FridgeRepository;
import com.example.cns.fridge.FridgeService;
import com.example.cns.fridge.UnitCategory;
import com.example.cns.ingredient.IngredientMaster;
import com.example.cns.ingredient.IngredientMasterRepository;
import com.example.cns.recipe.cashe.IngredientNameCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionAnalyzeService {

    private final GcpVisionClient gcpVisionClient;
    private final FridgeRepository fridgeRepository;
    private final FridgeService fridgeService;
    private final IngredientNameCache ingredientNameCache;
    private final IngredientParser ingredientParser;
    private final GoogleTranslateService googleTranslateService;
    private final IngredientMasterRepository ingredientMasterRepository;

    public List<String> analyzeAndSave(MultipartFile imageFile, UserEntity user) {
        List<String> detectedLabels = gcpVisionClient.detectLabels(imageFile); // 예: ["onion", "table", "apple"]
        log.debug("[VisionAnalyzeService] Vision 결과 라벨: {}", detectedLabels);

        List<String> savedIngredients = new ArrayList<>();

        // 전체 캐시된 재료명 가져오기
        Set<String> allKorNames = ingredientNameCache.getAll(); // 예: [양파, 당근, 우유]

        // 한글 → 영문 번역 (역매핑 준비)
        Map<String, String> korToEng = googleTranslateService.translateBatch(new ArrayList<>(allKorNames));
        log.debug("[VisionAnalyzeService] 번역 매핑 (한->영): {}", korToEng);
        // 영문 → 한글 역매핑 생성
        Map<String, String> engToKor = korToEng.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getValue().toLowerCase(),  // 영어 키
                        Map.Entry::getKey,                // 대응되는 한글 값
                        (existing, replacement) -> existing // 충돌 시 기존 값 유지
                ));
        log.debug("[VisionAnalyzeService] 역매핑 (영->한): {}", engToKor);
        long userId = (long) user.getId();

        for (String label : detectedLabels) {
            String labelLower = label.toLowerCase();

            if (engToKor.containsKey(labelLower)) {
                String korName = engToKor.get(labelLower); // 예: "onion" → "양파"

                Fridge fridge = new Fridge();
                fridge.setIngredientName(korName);
                fridge.setStorageArea("냉장");
                fridge.setQuantity(Double.valueOf(1.0D));
                fridge.setUnitCategory(UnitCategory.COUNT);
                fridge.setUnitDetail("개");
                fridge.setFridgeDate(LocalDate.now());
                fridge.setCreatedAt(LocalDateTime.now());
                fridge.setUpdatedAt(LocalDateTime.now());
                fridge.setUser(user);

                fridgeService.createFridge(fridge, Long.valueOf(userId)); // 히스토리도 같이 저장됨
                log.debug("[VisionAnalyzeService] 저장 시도: {}", korName);

                savedIngredients.add(korName);
                log.debug("[VisionAnalyzeService] 재료 저장 완료: {}", korName);

            }
        }

        return savedIngredients;
    }

    public List<IngredientDetectResponse> analyzeOnly(MultipartFile imageFile) {
        // 1️⃣ Vision 인식
        List<String> detectedLabels = gcpVisionClient.detectLabels(imageFile);
        log.debug("[VisionAnalyzeService] Vision 결과 라벨: {}", detectedLabels);

        // 2. 캐시된 한글 재료명 전체
        Set<String> allKorNames = ingredientNameCache.getAll();

        // 3️⃣ 한글 → 영어 번역
        Map<String, String> korToEng = googleTranslateService.translateBatch(new ArrayList<>(allKorNames));
        Map<String, String> engToKor = korToEng.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getValue().toLowerCase(),
                        Map.Entry::getKey,
                        (existing, replacement) -> existing
                ));

        List<IngredientDetectResponse> matchedIngredients = new ArrayList<>();

        // 4️⃣ Vision label → IngredientMaster 매칭
        for (String label : detectedLabels) {
            String labelLower = label.toLowerCase();
            if (engToKor.containsKey(labelLower)) {
                String korName = engToKor.get(labelLower);

                // DB에서 해당 재료 찾기
                IngredientMaster master = ingredientMasterRepository.findByNameKoIgnoreCase(korName).orElse(null);

                Long id = (master != null) ? master.getId() : null;

                matchedIngredients.add(IngredientDetectResponse.builder()
                        .ingredientId(id)
                        .nameKo(korName)
                        .build());

                log.debug("[analyzeOnly] 감지된 재료명: {} (id={})", korName, id);
            }
        }

        return matchedIngredients;
    }

}
