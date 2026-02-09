package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
public class TradePostListResponseDTO {
    private Long tradePostId;
    private String username;
    private String title;
    private LocalDateTime createdAt;
    private String category;
    private int status; // 0: 거래중, 1: 거래완료
}
