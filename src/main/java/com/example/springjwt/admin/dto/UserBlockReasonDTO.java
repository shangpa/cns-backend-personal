package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UserBlockReasonDTO {
    private String reason;         // 차단 사유
    private String blockedBy;      // 차단 관리자
    private LocalDateTime blockedAt; // 차단 일시
}
