package com.example.springjwt.fridge;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FridgeRequestDTO {
    private String ingredientName;
    private String storageArea;
    private LocalDate fridgeDate;
    private String dateOption;
    private Double quantity;
    private UnitCategory unitCategory;
    private String unitDetail;
    private Long userId;  // 로그인한 사용자의 ID

    // 기본 생성자
    public FridgeRequestDTO() {}

    // 모든 필드를 위한 생성자 (선택사항)
    public FridgeRequestDTO(String ingredientName, String storageArea, LocalDate fridgeDate, String dateOption, Double quantity, Double price, UnitCategory unitCategory, String unitDetail, Long userId) {
        this.ingredientName = ingredientName;
        this.storageArea = storageArea;
        this.fridgeDate = fridgeDate;
        this.dateOption = dateOption;
        this.quantity = quantity;
        this.unitCategory = unitCategory;
        this.unitDetail = unitDetail;
        this.userId = userId;
    }

}
