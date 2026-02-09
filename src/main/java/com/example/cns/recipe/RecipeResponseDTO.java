package com.example.cns.recipe;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeResponseDTO {
    private boolean success;
    private String message;
    private Long recipeId;
}

