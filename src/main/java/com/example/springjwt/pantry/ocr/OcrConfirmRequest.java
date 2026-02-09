package com.example.springjwt.pantry.ocr;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class OcrConfirmRequest {
    private List<Item> items;

    @Getter @Setter
    public static class Item {
        private String nameRaw;     // OCR에서 뽑은 원문 이름(백업용)
        private Long ingredientId;  // 프런트에서 매칭했으면 값, 아니면 null
        private String quantity;    // "2" / "1.5" (문자열)
        private Long unitId;        // null이면 마스터 기본 단위 사용
        private String storage;     // FRIDGE | FREEZER | PANTRY
        private String purchasedAt; // yyyy-MM-dd (nullable)
        private String expiresAt;   // yyyy-MM-dd (nullable)
    }
}
