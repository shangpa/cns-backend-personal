package com.example.springjwt.tradepost;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponseDTO {
    private String username;
    private double rating;
    private int reviewCount;
    private int transactionCount;
}