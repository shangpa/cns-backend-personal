package com.example.springjwt.pantry.use;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.ingredient.IngredientMaster;
import com.example.springjwt.ingredient.IngredientMasterRepository;
import com.example.springjwt.pantry.Pantry;
import com.example.springjwt.pantry.PantryRepository;
import com.example.springjwt.pantry.PantryStock;
import com.example.springjwt.pantry.PantryStockRepository;
import com.example.springjwt.pantry.history.HistoryAction;
import com.example.springjwt.pantry.history.PantryHistory;
import com.example.springjwt.pantry.history.PantryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PantryUseService {

    private final PantryRepository pantryRepo;
    private final IngredientMasterRepository ingRepo;
    private final PantryStockRepository stockRepo;
    private final PantryHistoryRepository histRepo;

    @Transactional
    public void useFromDefaultPantry(UserEntity user, List<UseIngredientRequest> reqs) {
        // 1) 기본 팬트리(없으면 첫 번째) 선택
        Pantry pantry = pantryRepo.findAllByUser_IdOrderBySortOrderAscCreatedAtAsc(user.getId())
                .stream()
                .filter(Pantry::isDefault)
                .findFirst()
                .orElseGet(() -> pantryRepo.findAllByUser_IdOrderBySortOrderAscCreatedAtAsc(user.getId())
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("기본 냉장고가 없습니다.")));

        for (UseIngredientRequest r : reqs) {
            // 2) 재료 조회 (이름 대소문자 무시)
            IngredientMaster ing = ingRepo.findByNameKoIgnoreCase(r.getName())
                    .orElseThrow(() -> new IllegalArgumentException("재료 없음: " + r.getName()));

            // 3) 남은 차감량(소수 3자리 고정)
            BigDecimal remain = BigDecimal.valueOf(r.getAmount())
                    .setScale(3, RoundingMode.HALF_UP);

            // 4) 해당 재료 재고: 유통기한 오름차순 → 생성일 오름차순
            List<PantryStock> stocks = stockRepo.findAllByPantry_Id(pantry.getId()).stream()
                    .filter(s -> s.getIngredient().getId().equals(ing.getId()))
                    .sorted(Comparator
                            .comparing(PantryStock::getExpiresAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(PantryStock::getCreatedAt))
                    .toList();

            for (PantryStock s : stocks) {
                if (remain.signum() <= 0) break;

                BigDecimal q = s.getQuantity();
                BigDecimal consume = q.min(remain);

                boolean willBeZero = q.compareTo(consume) == 0;

                // 5) 히스토리 먼저 기록
                PantryHistory history = PantryHistory.builder()
                        .pantry(pantry)
                        .ingredient(ing)
                        .unit(s.getUnit())
                        .changeQty(consume.negate())
                        .action(HistoryAction.USE)
                        .stock(willBeZero ? null : s)     // ★ 전량 소진 시 stock 참조 금지(핵심)
                        .refType("RECIPE_USE")
                        .note(willBeZero ? "사용(전량 소진)" : "사용")
                        .purchasedAt(s.getPurchasedAt())
                        .expiresAt(s.getExpiresAt())
                        .build();
                histRepo.save(history);

                // 6) 재고 반영
                if (willBeZero) {
                    s.setQuantity(BigDecimal.ZERO);
                    stockRepo.save(s);  // ← delete 대신 save
                } else {
                    s.setQuantity(q.subtract(consume));
                    stockRepo.save(s);
                }

                remain = remain.subtract(consume);
            }

            if (remain.signum() > 0) {
                // 남은 차감량이 있으면 재고 부족
                throw new IllegalStateException("재고 부족: " + ing.getNameKo() + " " + remain.toPlainString());
            }
        }
        // 주의: 0이 된 재고는 위에서 바로 삭제 처리함
    }
}
