package com.example.springjwt.pantry.ocr;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OcrConfirmResult {
    private String nameRaw;
    private String status;    // OK | ERROR
    private Long stockId;     // 생성/병합된 재고 ID
    private String message;   // 에러 메시지(에러일 때)
}
