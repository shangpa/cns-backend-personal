package com.example.cns.ingredient.dto;

import com.example.cns.ingredient.IngredientMaster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder @AllArgsConstructor
public class IngredientMasterResponse {
    private Long id;
    private String nameKo;
    private String category;     // enum name()
    private Long defaultUnitId;  // nullable
    private String iconUrl;      // nullable

    public static IngredientMasterResponse fromEntity(IngredientMaster it) {
        return IngredientMasterResponse.builder()
                .id(it.getId())
                .nameKo(it.getNameKo())
                .category(it.getCategory().name())
                .defaultUnitId(it.getDefaultUnit() == null ? null : it.getDefaultUnit().getId())
                .iconUrl(it.getIconUrl())
                .build();
    }
}