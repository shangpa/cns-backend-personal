package com.example.springjwt.point;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class PointHistoryResponseDTO {
    private String action;
    private int pointChange;
    private String description;
    private LocalDateTime createdAt;
}
