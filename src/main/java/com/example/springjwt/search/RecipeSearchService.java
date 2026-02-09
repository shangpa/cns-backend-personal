package com.example.springjwt.search;

import com.example.springjwt.recipe.Recipe;
import com.example.springjwt.recipe.RecipeCategory;
import com.example.springjwt.recipe.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeSearchService {

    private final RecipeRepository repo;

    public Page<Recipe> search(String title, String category, SortKey sortKey, int page, int size) {
        Sort sort = Sort.by(sortKey.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortKey.property());
        Pageable pageable = PageRequest.of(page, size, sort);

        RecipeCategory cat = parseCategory(category);
        if (cat == null) {
            return repo.findByIsPublicTrueAndTitleContainingIgnoreCase(title, pageable);
        }
        return repo.findByIsPublicTrueAndTitleContainingIgnoreCaseAndCategory(title, cat, pageable);
    }

    private RecipeCategory parseCategory(String c) {
        if (c == null || c.isBlank()) return null;
        try { return RecipeCategory.valueOf(c); } catch (IllegalArgumentException e) { return null; }
    }
}
