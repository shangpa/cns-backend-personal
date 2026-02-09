package com.example.springjwt.fridge.recommend;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecipeRecommendRequestDTO {
    private List<Long> selectedIngredientIds;
}
