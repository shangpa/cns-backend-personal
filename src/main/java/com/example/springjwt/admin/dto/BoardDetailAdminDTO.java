package com.example.springjwt.admin.dto;

import com.example.springjwt.board.BoardType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class BoardDetailAdminDTO {
    private Long id;
    private String writer;
    private String content;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private BoardType boardType;
    private List<BoardCommentResponseDTO> comments;
}
