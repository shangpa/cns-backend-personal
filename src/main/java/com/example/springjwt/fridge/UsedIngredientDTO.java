package com.example.springjwt.fridge;

import lombok.Data;

@Data
public class UsedIngredientDTO {
    private String name;
    private double amount;  // 차감할 수량
}