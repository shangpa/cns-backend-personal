package com.example.springjwt.recipe.expected;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExpectedIngredientResponse {
    private Long ingredientId;
    private String name;              // IngredientMaster.nameKo
    private String unit;              // IngredientMaster.defaultUnit.name (nullable)
    private String amountInRecipe;    // Double → 문자열 (stripTrailingZeros)
    private String amountInFridge;    // fridge 미사용이므로 "0"
    private String dateOption;        // ""
    private String date;              // ""
    private String iconUrl;           // IngredientMaster.iconUrl (nullable)
}