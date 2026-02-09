package com.example.springjwt.admin.dto;

import com.example.springjwt.ingredient.IngredientCategory;
import lombok.Data;

@Data
public class IngredientAdminRequest {
    private String nameKo;
    private IngredientCategory category;
    private Long defaultUnitId;
    private String iconUrl;
}
