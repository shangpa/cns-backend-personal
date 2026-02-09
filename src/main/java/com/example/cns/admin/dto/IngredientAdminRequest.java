package com.example.cns.admin.dto;

import com.example.cns.ingredient.IngredientCategory;
import lombok.Data;

@Data
public class IngredientAdminRequest {
    private String nameKo;
    private IngredientCategory category;
    private Long defaultUnitId;
    private String iconUrl;
}
