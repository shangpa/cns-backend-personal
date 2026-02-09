package com.example.springjwt.recipe.expected;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExpectedIngredientDTO {
    private String name;
    private String amountInRecipe;
    private String amountInFridge;
    private String unit;
    private String date;
    private String dateOption;
}
