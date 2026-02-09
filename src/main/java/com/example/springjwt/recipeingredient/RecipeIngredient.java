package com.example.springjwt.recipeingredient;

import com.example.springjwt.ingredient.IngredientMaster;
import com.example.springjwt.ingredient.UnitEntity;
import com.example.springjwt.recipe.Recipe;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recipe_ingredient")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 레시피의 재료인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    /** 어떤 마스터 재료인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private IngredientMaster ingredient;

    /** 수량 */
    @Column(nullable = false)
    private Double quantity;
}