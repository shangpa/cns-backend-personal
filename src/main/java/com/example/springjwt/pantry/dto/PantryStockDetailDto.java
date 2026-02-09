// PantryStockDetailDto.java  (조회용)
package com.example.springjwt.pantry.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PantryStockDetailDto {
    private Long id;
    private String ingredientName;
    private String category;     // enum name
    private String storage;      // FRIDGE/FREEZER/PANTRY
    private String quantity;     // BigDecimal -> toPlainString()
    private Long unitId;
    private Long ingredientId;
    private String unitName;     // 표시용
    private String iconUrl;
    private String purchasedAt;  // yyyy-MM-dd (nullable)
    private String expiresAt;
}