package com.example.springjwt.recipe.cashe;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IngredientNameCache {

    private final Set<String> ingredientNames = ConcurrentHashMap.newKeySet();

    public void initialize(List<String> allNames) {
        ingredientNames.clear();
        ingredientNames.addAll(allNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet()));
    }

    public void addFromNames(List<String> names) {
        ingredientNames.addAll(
                names.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet())
        );
    }

    public boolean contains(String name) {
        return ingredientNames.contains(name.toLowerCase());
    }

    public Set<String> getAll() {
        return Set.copyOf(ingredientNames);
    }
}