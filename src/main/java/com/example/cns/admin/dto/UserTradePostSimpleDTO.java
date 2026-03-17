package com.example.cns.admin.dto;

import com.example.cns.tradepost.TradeStatus;
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
    private TradeStatus status; // ONGOING: 거래중, COMPLETED: 거래완료
}
