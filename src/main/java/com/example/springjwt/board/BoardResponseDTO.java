package com.example.springjwt.board;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class BoardResponseDTO {private Long id;
    private String content;
    private String writer;
    private List<String> imageUrls;
    private String boardType;
    private LocalDateTime createdAt;
}
