package com.example.springjwt.notification;

import lombok.Data;

@Data
public class FcmTokenRequestDTO {
    private String token;
    private String platform;
}