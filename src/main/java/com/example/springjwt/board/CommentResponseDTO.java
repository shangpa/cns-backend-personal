package com.example.springjwt.board;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private String user;
    private String content;
    private String createdAt;
}