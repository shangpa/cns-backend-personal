package com.example.springjwt.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDTO {

    private String category;
    private String content;
    private LocalDateTime createdAt;
    private boolean isRead;

    public static NotificationResponseDTO fromEntity(NotificationEntity entity) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setCategory(entity.getCategory());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setRead(entity.isRead());
        return dto;
    }
}
