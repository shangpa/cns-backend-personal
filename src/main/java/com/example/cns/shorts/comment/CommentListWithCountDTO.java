package com.example.cns.shorts.comment;
import com.example.cns.board.CommentResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CommentListWithCountDTO {
    private List<CommentResponseDTO> comments;
    private int count;
}