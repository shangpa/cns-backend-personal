package com.example.springjwt.ingredient.dto;

import com.example.springjwt.ingredient.UnitEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder @AllArgsConstructor
public class UnitResponse {
    private Long id;
    private String name;  // g, kg, ml, L, 개 …
    private String kind;  // WEIGHT/VOLUME/COUNT

    public static UnitResponse fromEntity(UnitEntity u) {
        return UnitResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .kind(u.getKind().name())
                .build();
    }
}