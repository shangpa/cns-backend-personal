package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardAdminListResponseDTO {
    private Long id;
    private String writer;  // 작성자 아이디 or 닉네임
    private String content;
    private LocalDateTime createdAt;
}
