package com.example.springjwt.api.vision;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class IngredientParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> extractNames(String json) {
        try {
            List<IngredientDTO> ingredients = objectMapper.readValue(
                    json, new TypeReference<List<IngredientDTO>>() {}
            );
            return ingredients.stream()
                    .map(IngredientDTO::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("레시피 재료 파싱 실패: " + e.getMessage(), e);
        }
    }
}
