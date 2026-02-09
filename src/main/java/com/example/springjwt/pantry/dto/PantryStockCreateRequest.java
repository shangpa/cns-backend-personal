package com.example.springjwt.pantry.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class PantryStockCreateRequest {
    private Long ingredientId;
    private Long unitId;
    private String quantity;     // "1.0"
    private String storage;      // FRIDGE/FREEZER/PANTRY
    private String purchasedAt;  // yyyy-MM-dd (nullable)
    private String expiresAt;    // yyyy-MM-dd (nullable)
    private String memo;         // nullable
    private String source;       // MANUAL/OCR/RECOMMEND/IMPORT (nullable)
}