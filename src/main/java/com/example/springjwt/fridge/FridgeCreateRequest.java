package com.example.springjwt.fridge;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FridgeCreateRequest {
    private String ingredientName;
    private Double quantity;
    private String fridgeDate; // yyyy-MM-dd
    private String dateOption;
    private String storageArea;
    private String unitDetail;
    private String unitCategory;
}