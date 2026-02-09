package com.example.springjwt.recipe.expected;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.recipeingredient.RecipeIngredient;
import com.example.springjwt.recipeingredient.RecipeIngredientRepository;
import com.example.springjwt.pantry.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeExpectedIngredientService {

    private final RecipeIngredientRepository recipeIngredientRepository;
    private final PantryRepository pantryRepository;
    private final PantryStockRepository pantryStockRepository;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ExpectedIngredientResponse> getExpectedIngredients(Long recipeId, UserEntity user) {
        // 1) 레시피 재료 조회
        List<RecipeIngredient> rows;
        try {
            rows = recipeIngredientRepository.findByRecipe_RecipeId(recipeId);
        } catch (Exception ignore) {
            rows = recipeIngredientRepository.findByRecipe_RecipeId(recipeId);
        }

        // 2) 기본 팬트리 선택 (없으면 첫 번째, 그래도 없으면 null)
        var defaultPantry = pantryRepository.findFirstByUser_IdAndIsDefaultTrue(user.getId())
                .orElseGet(() -> pantryRepository.findAllByUser_IdOrderBySortOrderAscCreatedAtAsc(user.getId())
                        .stream().findFirst().orElse(null));

        return rows.stream().map(ri -> {
            var ing = ri.getIngredient();
            var defaultUnit = (ing != null && ing.getDefaultUnit() != null) ? ing.getDefaultUnit() : null;

            // 필요 수량(1인 기준)
            String need = toPlain(ri.getQuantity());

            // 3) 보유 수량 & 날짜 (기본 팬트리 기준)
            String have = "0";
            String dateOption = "";
            String date = "";
            if (defaultPantry != null && ing != null) {
                var stocks = pantryStockRepository
                        .findAllByPantry_IdAndIngredient_Id(defaultPantry.getId(), ing.getId());

                // 단위 일치하는 재고만 합산 (변환 로직이 없으므로 안전하게)
                BigDecimal sum = BigDecimal.ZERO;
                LocalDate minExpires = null;
                LocalDate minPurchased = null;

                for (var s : stocks) {
                    // 같은 단위(기본 단위)만 집계
                    if (defaultUnit != null && s.getUnit() != null
                            && Objects.equals(defaultUnit.getId(), s.getUnit().getId())) {
                        if (s.getQuantity() != null) sum = sum.add(s.getQuantity());

                        var ex = s.getExpiresAt();
                        var pu = s.getPurchasedAt();
                        if (ex != null) minExpires = (minExpires == null || ex.isBefore(minExpires)) ? ex : minExpires;
                        if (pu != null) minPurchased = (minPurchased == null || pu.isBefore(minPurchased)) ? pu : minPurchased;
                    }
                }

                have = stripZeros(sum);

                if (minExpires != null) {
                    dateOption = "유통기한";
                    date = DF.format(minExpires);
                } else if (minPurchased != null) {
                    dateOption = "구매일자";
                    date = DF.format(minPurchased);
                }
            }

            return ExpectedIngredientResponse.builder()
                    .ingredientId(ing != null ? ing.getId() : null)
                    .name(ing != null ? nz(ing.getNameKo()) : "")
                    .unit(defaultUnit != null ? defaultUnit.getName() : null)
                    .amountInRecipe(need)
                    .amountInFridge(have)    // ✅ 합산 결과
                    .dateOption(dateOption)  // ✅ 유통기한 or 구매일자
                    .date(date)
                    .iconUrl(ing != null ? ing.getIconUrl() : null)
                    .build();
        }).toList();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String toPlain(Double d) {
        if (d == null) return "0";
        return new BigDecimal(String.valueOf(d)).stripTrailingZeros().toPlainString();
    }

    private static String stripZeros(BigDecimal bd) {
        if (bd == null) return "0";
        return bd.stripTrailingZeros().toPlainString();
    }
}
