package com.example.springjwt.notification;

import lombok.Data;

@Data
public class NotificationRequestDTO {
    private int userId;
    private String category;
    private String content;
}