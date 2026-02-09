package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserListDTO {
    private int id;
    private String name;
    private String username;
}