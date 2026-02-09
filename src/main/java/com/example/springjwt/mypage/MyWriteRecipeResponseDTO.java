package com.example.springjwt.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class MyWriteRecipeResponseDTO {
    private int count;
    private List<MyWriteRecipeDTO> recipes;
}