package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecipeStatsResponseDTO {
    private List<RecipeStatDTO> data;
    private RecipeSummaryDTO summary;
}