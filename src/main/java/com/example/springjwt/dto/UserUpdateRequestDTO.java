package com.example.springjwt.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequestDTO {
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private String address;
}
