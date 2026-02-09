package com.example.springjwt.recipe.expected;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeExpectedIngredientController {

    private final RecipeExpectedIngredientService service;

    @GetMapping("/{id}/expected-ingredients")
    public ResponseEntity<List<ExpectedIngredientResponse>> expectedIngredients(
            @PathVariable("id") Long recipeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserEntity user = ((CustomUserDetails) userDetails).getUserEntity();
        return ResponseEntity.ok(service.getExpectedIngredients(recipeId, user));
    }
}
