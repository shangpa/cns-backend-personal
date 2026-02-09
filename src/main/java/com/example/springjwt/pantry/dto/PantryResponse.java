package com.example.springjwt.pantry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PantryResponse {
    private Long id;
    private String name;
    private String note;
    private String imageUrl;
    private boolean isDefault;
    private int sortOrder;
    private LocalDateTime createdAt;
}
