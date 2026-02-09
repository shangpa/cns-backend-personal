package com.example.springjwt.pantry.history;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.pantry.Pantry;
import com.example.springjwt.pantry.PantryRepository;
import com.example.springjwt.pantry.dto.PantryHistoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PantryHistoryService {

    private final PantryRepository pantryRepository;
    private final PantryHistoryRepository historyRepository;

    private static final DateTimeFormatter DF_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DF_DT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public List<PantryHistoryDto> list(UserEntity user, Long pantryId, String ingredientName) {
        Pantry pantry = pantryRepository.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다. id=" + pantryId));

        var rows = (ingredientName == null || ingredientName.isBlank())
                ? historyRepository.findByPantry_IdOrderByCreatedAtDesc(pantry.getId())
                : historyRepository.findByPantry_IdAndIngredient_NameKoContainingIgnoreCaseOrderByCreatedAtDesc(
                pantry.getId(), ingredientName.trim()
        );

        return rows.stream().map(h -> {
            var ing   = h.getIngredient();
            var unit  = h.getUnit();
            var stock = h.getStock();

            // 1순위: 히스토리 자체에 저장된 날짜
            String purchasedAt = (h.getPurchasedAt() != null) ? DF_DATE.format(h.getPurchasedAt()) : null;
            String expiresAt   = (h.getExpiresAt()   != null) ? DF_DATE.format(h.getExpiresAt())   : null;

            // 2순위: 과거 데이터 호환 - 히스토리에 날짜가 없으면 stock에서 끌어오기
            if (purchasedAt == null && stock != null && stock.getPurchasedAt() != null) {
                purchasedAt = DF_DATE.format(stock.getPurchasedAt());
            }
            if (expiresAt == null && stock != null && stock.getExpiresAt() != null) {
                expiresAt = DF_DATE.format(stock.getExpiresAt());
            }

            return PantryHistoryDto.builder()
                    .id(h.getId())
                    .action(h.getAction().name())
                    .ingredientId(ing.getId())
                    .ingredientName(ing.getNameKo())
                    .category(ing.getCategory() != null ? ing.getCategory().name() : null)
                    .quantity(h.getChangeQty().toPlainString())
                    .unitId(unit.getId())
                    .unitName(unit.getName())
                    .purchasedAt(purchasedAt)
                    .expiresAt(expiresAt)
                    .createdAt(DF_DT.format(h.getCreatedAt()))
                    .iconUrl(ing.getIconUrl())
                    .build();
        }).toList();
    }
}
