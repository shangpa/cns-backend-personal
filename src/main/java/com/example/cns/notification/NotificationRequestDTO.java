package com.example.cns.notification;

import lombok.Data;

@Data
public class NotificationRequestDTO {
    private int userId;
    private String category;
    private String content;
}