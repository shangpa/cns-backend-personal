package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserDetailDTO {
    private int id;
    private String name;
    private String username;
    private LocalDateTime createdAt;
    private int point;
    private int recipeCount;
    private int tradePostCount;
    private int reviewCount;
}
