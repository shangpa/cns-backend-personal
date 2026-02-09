package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecipeStatDTO {
    private String label; // 예: "6월", "12일", "2025-06-03"
    private long count;
}
