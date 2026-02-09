package com.example.springjwt.board;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CommentListWithCountDTO {
    private List<CommentResponseDTO> comments;
    private int count;
}