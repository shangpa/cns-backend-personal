package com.example.springjwt.pantry.ocr;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.ingredient.IngredientMaster;
import com.example.springjwt.ingredient.IngredientMasterRepository;
import com.example.springjwt.ingredient.UnitEntity;
import com.example.springjwt.ingredient.UnitRepository;
import com.example.springjwt.pantry.*;
import com.example.springjwt.pantry.history.HistoryAction;
import com.example.springjwt.pantry.history.PantryHistory;
import com.example.springjwt.pantry.history.PantryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PantryOcrService {

    private final PantryRepository pantryRepo;
    private final IngredientMasterRepository ingRepo;
    private final UnitRepository unitRepo;
    private final PantryStockRepository stockRepo;
    private final PantryHistoryRepository histRepo;

    /**
     * OCR 인식된 항목을 냉장고에 추가.
     * 각 항목별로 try-catch 하여 부분 성공 허용.
     */
    @Transactional
    public List<OcrConfirmResult> confirm(UserEntity user, Long pantryId, OcrConfirmRequest req) {
        Pantry pantry = pantryRepo.findByIdAndUser_Id(pantryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다. id=" + pantryId));

        List<OcrConfirmResult> out = new ArrayList<>();
        if (req.getItems() == null || req.getItems().isEmpty()) return out;

        for (OcrConfirmRequest.Item it : req.getItems()) {
            try {
                // 1) 재료 찾기
                IngredientMaster ing = (it.getIngredientId() != null)
                        ? ingRepo.findById(it.getIngredientId())
                        .orElseThrow(() -> new IllegalArgumentException("재료 없음: id=" + it.getIngredientId()))
                        : ingRepo.findByNameKoIgnoreCase(cleanName(it.getNameRaw()))
                        .orElseThrow(() -> new IllegalArgumentException("재료 이름 매칭 실패: " + it.getNameRaw()));

                // 2) 단위 선택
                UnitEntity unit = (it.getUnitId() != null)
                        ? unitRepo.findById(it.getUnitId())
                        .orElseThrow(() -> new IllegalArgumentException("단위 없음: id=" + it.getUnitId()))
                        : Optional.ofNullable(ing.getDefaultUnit())
                        .orElseThrow(() -> new IllegalArgumentException("단위가 지정되지 않았고 기본 단위도 없습니다: " + ing.getNameKo()));

                // 3) 수량/보관/날짜 파싱
                BigDecimal qty = parseQty(it.getQuantity());
                StorageLocation storage = parseStorage(it.getStorage());
                LocalDate purchasedAt = parseDate(it.getPurchasedAt());
                LocalDate expiresAt   = parseDate(it.getExpiresAt());

                // 4) 업서트(기존 있으면 수량 합산, 없으면 새로 생성)
                PantryStock saved = upsertAndAddQuantity(
                        pantry.getId(), ing.getId(), unit.getId(),
                        storage, purchasedAt, expiresAt, qty, "OCR"
                );

                // 5) 히스토리 기록
                histRepo.save(PantryHistory.builder()
                        .pantry(pantry)
                        .ingredient(ing)
                        .unit(unit)
                        .changeQty(qty)
                        .action(HistoryAction.ADD)
                        .stock(saved)
                        .refType("PANTRY_STOCK")
                        .refId(saved.getId())
                        .note("영수증(OCR) 추가")
                        .purchasedAt(purchasedAt)
                        .expiresAt(expiresAt)
                        .build());

                out.add(new OcrConfirmResult(it.getNameRaw(), "OK", saved.getId(), null));

            } catch (Exception e) {
                out.add(new OcrConfirmResult(it.getNameRaw(), "ERROR", null, e.getMessage()));
            }
        }
        return out;
    }

    /** 업서트: 동일 키 재고가 있으면 수량 합산, 없으면 새로 생성 */
    @Transactional
    public PantryStock upsertAndAddQuantity(Long pantryId, Long ingredientId, Long unitId,
                                            StorageLocation storage, LocalDate purchasedAt, LocalDate expiresAt,
                                            BigDecimal addQty, String source) {

        var opt = stockRepo.findByPantry_IdAndIngredient_IdAndUnit_IdAndStorageAndPurchasedAtAndExpiresAt(
                pantryId, ingredientId, unitId, storage, purchasedAt, expiresAt
        );

        PantryStock s = opt.orElseGet(() -> {
            PantryStock ns = new PantryStock();
            ns.setPantry(pantryRepo.getReferenceById(pantryId));
            ns.setIngredient(ingRepo.getReferenceById(ingredientId));
            ns.setUnit(unitRepo.getReferenceById(unitId));
            ns.setStorage(storage);
            ns.setPurchasedAt(purchasedAt);
            ns.setExpiresAt(expiresAt);
            ns.setSource(StockSource.valueOf(source)); // OCR, MANUAL, IMPORT 등
            ns.setQuantity(BigDecimal.ZERO);
            return ns;
        });

        s.setQuantity(s.getQuantity().add(addQty));
        return stockRepo.save(s);
    }

    // ================== 유틸 ==================

    private static BigDecimal parseQty(String q) {
        String v = (q == null ? "" : q.trim());
        if (v.isEmpty()) v = "1";
        try {
            BigDecimal bd = new BigDecimal(v);
            if (bd.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("수량은 0보다 커야 합니다: " + v);
            return bd.setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("수량 형식 오류: " + v);
        }
    }

    private static StorageLocation parseStorage(String s) {
        if (s == null || s.isBlank()) return StorageLocation.FRIDGE;
        try {
            return StorageLocation.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("storage 값 오류: " + s);
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜 형식 오류(yyyy-MM-dd): " + s);
        }
    }

    /** OCR 텍스트의 잡음 제거 */
    private static String cleanName(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\(\\)\\[\\]\\*\\+]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
