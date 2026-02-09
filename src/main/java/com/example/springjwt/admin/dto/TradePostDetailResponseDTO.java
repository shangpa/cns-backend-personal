package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TradePostDetailResponseDTO {
    private Long tradePostId;
    private String username;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private String location;
    private int chatCount;
    private int viewCount;
    private int status; // 0: 거래중, 1: 거래완료

}
