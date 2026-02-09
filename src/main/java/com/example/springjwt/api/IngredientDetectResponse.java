package com.example.springjwt.api;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientDetectResponse {
    private Long ingredientId;   // IngredientMaster id
    private String nameKo;       // 한글 이름 (ex: 감자)
    private String iconUrl;      // 아이콘 URL
}
