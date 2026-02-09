// PantryStockUpdateRequest.java  (수정용)
package com.example.springjwt.pantry.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PantryStockUpdateRequest {
    private String quantity;     // nullable → 전달 시만 수정
    private String storage;      // nullable
    private String purchasedAt;  // nullable
    private String expiresAt;    // nullable  ⬅⬅ 추가
}