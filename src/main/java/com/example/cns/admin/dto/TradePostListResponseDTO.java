package com.example.cns.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import com.example.cns.tradepost.TradeStatus;

import java.time.LocalDateTime;

@Getter @Setter
@AllArgsConstructor
public class TradePostListResponseDTO {
    private Long tradePostId;
    private String username;
    private String title;
    private LocalDateTime createdAt;
    private String category;
    private TradeStatus status; // ONGOING: 거래중, COMPLETED: 거래완료
}
