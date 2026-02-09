package com.example.springjwt.pantry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PantryHistoryDto {
    private Long id;

    // "ADD" | "USE" | "DISCARD" | "ADJUST"
    private String action;

    private Long ingredientId;
    private String ingredientName;
    private String category;     // enum name or null

    private String quantity;     // changeQty.toPlainString()  (부호 포함: +입고 / -사용·폐기·조정)
    private Long unitId;
    private String unitName;

    private String purchasedAt;  // yyyy-MM-dd (nullable; stock에서 끌어옴 가능)
    private String expiresAt;    // yyyy-MM-dd (nullable)
    private String createdAt;    // yyyy-MM-dd HH:mm:ss

    private String iconUrl;
}
