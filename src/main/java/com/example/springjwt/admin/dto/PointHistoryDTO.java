package com.example.springjwt.admin.dto;

import com.example.springjwt.point.PointHistory;
import com.example.springjwt.point.PointActionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointHistoryDTO {
    private Long id;
    private String action; // 적립 유형
    private int pointChange;
    private String description;
    private LocalDateTime createdAt;

    public static PointHistoryDTO from(PointHistory entity) {
        PointHistoryDTO dto = new PointHistoryDTO();
        dto.setId(entity.getId());
        dto.setAction(entity.getAction().name());
        dto.setPointChange(entity.getPointChange());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
