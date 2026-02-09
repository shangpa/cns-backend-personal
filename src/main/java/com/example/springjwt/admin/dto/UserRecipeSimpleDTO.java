package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserRecipeSimpleDTO {
    private Long recipeId;
    private String username;
    private String title;
    private LocalDateTime createdAt;
}
