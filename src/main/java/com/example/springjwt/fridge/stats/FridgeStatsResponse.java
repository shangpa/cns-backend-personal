package com.example.springjwt.fridge.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FridgeStatsResponse {
    private long nowCount;   // 지금 보관중
    private long eatenCount; // 유통기한 내에 먹은 수
    private long expiredCount; // 유통기한 지키지 못한 수

    private long refrigeratedCount; // 냉장
    private long frozenCount;       // 냉동
    private long roomCount;         // 실온
}