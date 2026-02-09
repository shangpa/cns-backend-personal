package com.example.springjwt.pantry.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PantryStockDto {
    private Long id;

    private Long ingredientId;
    private Long unitId;
    private String unitName;

    private String ingredientName;
    private String category;
    private String storage;

    private String quantity;
    private String iconUrl;

    private String purchasedAt;
    private String expiresAt;
}