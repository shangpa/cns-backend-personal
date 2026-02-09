package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UserBlockedListDTO {
    private int id;
    private String name;
    private String username;
    private LocalDateTime blockedAt;
}
