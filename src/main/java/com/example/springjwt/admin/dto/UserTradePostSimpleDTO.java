package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserTradePostSimpleDTO {
    private Long tradePostId;
    private String title;
    private String shortDescription;
    private LocalDateTime createdAt;
    private int status; // 0: 거래중, 1: 거래완료
}
