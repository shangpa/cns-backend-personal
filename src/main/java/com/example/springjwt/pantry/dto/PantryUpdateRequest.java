package com.example.springjwt.pantry.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class PantryUpdateRequest {
    private String name;        // 변경할 이름(선택)
    private String note;        // 변경할 메모(선택)
    private String imageUrl;    // 변경할 이미지(선택)
    private Boolean isDefault;  // 기본 여부(선택)
    private Integer sortOrder;  // 정렬순서(선택)
}
