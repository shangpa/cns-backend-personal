package com.example.springjwt.notification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSendRequestDTO {
    private String title;
    private String content;
    private String category; // "RECIPE", "FRIDGE", "VILLAGE", "COMMUNITY"
    private int userId;
}
