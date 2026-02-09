package com.example.springjwt.board;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BoardDetailResponseDTO {
    private Long id;
    private String content;
    private String writer;
    private List<String> imageUrls;
    private String boardType;
    private String createdAt;
    private int likeCount;
    private boolean liked;
    private int commentCount;

    public static BoardDetailResponseDTO from(Board board) {
        return new BoardDetailResponseDTO(
                board.getId(),
                board.getContent(),
                board.getWriter().getUsername(),
                board.getImageUrls(),
                board.getBoardType().toString(),
                board.getCreatedAt().toString(),
                board.getLikeCount(),
                false, // 또는 좋아요 여부 처리
                board.getCommentCount()
        );
    }


}