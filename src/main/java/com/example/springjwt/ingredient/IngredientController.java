package com.example.springjwt.ingredient;

import com.example.springjwt.ingredient.dto.IngredientMasterResponse;
import com.example.springjwt.ingredient.dto.UnitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping("/ingredients/categories")
    public ResponseEntity<List<String>> categories() {
        var list = Arrays.stream(IngredientCategory.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/ingredients")
    public ResponseEntity<List<IngredientMasterResponse>> list(
            @RequestParam String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(ingredientService.getIngredientsByCategory(category, keyword, page, size));
    }

    @GetMapping("/ingredients/all")
    public ResponseEntity<List<IngredientMasterResponse>> listAll() {
        return ResponseEntity.ok(ingredientService.getAllIngredients());
    }

    @GetMapping("/units")
    public ResponseEntity<List<UnitResponse>> units() {
        return ResponseEntity.ok(ingredientService.getUnits());
    }

    @GetMapping("/ingredients/search")
    public ResponseEntity<List<IngredientMasterResponse>> search(@RequestParam String keyword) {
        var rows = ingredientService.searchByName(keyword);
        return ResponseEntity.ok(rows);
    }
}
