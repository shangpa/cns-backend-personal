package com.example.springjwt.shorts.comment;
import com.example.springjwt.board.CommentResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CommentListWithCountDTO {
    private List<CommentResponseDTO> comments;
    private int count;
}