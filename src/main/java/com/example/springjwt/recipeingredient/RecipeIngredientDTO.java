package com.example.springjwt.recipeingredient;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeIngredientDTO {
    private Long id;   // IngredientMaster.id
    private String name;       // IngredientMaster.nameKo
    private Double amount;     // 사용자가 입력한 수량

    /** Entity -> DTO */
    public static RecipeIngredientDTO fromEntity(RecipeIngredient ri) {
        return RecipeIngredientDTO.builder()
                .id(ri.getIngredient().getId())
                .name(ri.getIngredient().getNameKo())
                .amount(ri.getQuantity())
                .build();
    }
}
