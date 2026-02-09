package com.example.springjwt.admin.dto;

import com.example.springjwt.board.BoardComment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentAdminResponseDTO {
    private Long commentId;
    private String content;
    private String writer;
    private LocalDateTime createdAt;

    public static CommentAdminResponseDTO from(BoardComment comment) {
        return new CommentAdminResponseDTO(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getUsername(),
                comment.getCreatedAt()
        );
    }
}
