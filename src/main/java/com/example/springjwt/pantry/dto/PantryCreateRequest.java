package com.example.springjwt.pantry.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PantryCreateRequest {
    private String name;       // 필수(서비스에서 빈값 체크 가능), 중복 불가(유저 내)
    private String note;       // 메모
    private String imageUrl;   // 이미지 URL
    private Boolean isDefault; // 기본 냉장고 여부
    private Integer sortOrder; // 정렬순서 (null이면 0 처리)
}
