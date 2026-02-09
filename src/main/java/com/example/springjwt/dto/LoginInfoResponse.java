package com.example.springjwt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginInfoResponse {
    private Long id;
    private String username;
    private String name;
    private String profileImageUrl;
}
