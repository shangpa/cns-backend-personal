package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DeletedLogDTO {
    private String adminUsername;
    private String action;
    private String targetType;
    private Long targetId;
    private String reason;
    private LocalDateTime createdAt;
}
