package com.example.springjwt.ingredient;

import com.example.springjwt.ingredient.dto.IngredientMasterResponse;
import com.example.springjwt.ingredient.dto.UnitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientMasterRepository ingredientRepo;
    private final UnitRepository unitRepo;

    public List<IngredientMasterResponse> getAllIngredients() {
        return ingredientRepo.findAll().stream()
                .map(IngredientMasterResponse::fromEntity)
                .toList();
    }

    public List<IngredientMasterResponse> getIngredientsByCategory(String category, String keyword, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var cat = IngredientCategory.valueOf(category);

        var pageResult = (keyword == null || keyword.isBlank())
                ? ingredientRepo.findByCategory(cat, pageable)
                : ingredientRepo.findByCategoryAndNameKoContainingIgnoreCase(cat, keyword.trim(), pageable);

        return pageResult.getContent().stream()
                .map(IngredientMasterResponse::fromEntity)
                .toList();
    }

    public List<UnitResponse> getUnits() {
        return unitRepo.findAll().stream()
                .map(UnitResponse::fromEntity)
                .toList();
    }

    public List<IngredientMasterResponse> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return ingredientRepo.findTop20ByNameKoContainingIgnoreCaseOrderByNameKoAsc(keyword.trim())
                .stream().map(IngredientMasterResponse::fromEntity).toList();
    }
}
