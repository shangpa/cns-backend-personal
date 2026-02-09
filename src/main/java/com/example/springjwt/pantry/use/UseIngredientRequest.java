package com.example.springjwt.pantry.use;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UseIngredientRequest {
    private String name;   // 재료 이름(IngredientMaster.nameKo)
    private Double amount; // 차감 수량 (단위 변환 없음, 마스터 기본 단위로 가정)
}