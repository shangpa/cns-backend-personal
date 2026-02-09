package com.example.springjwt.board;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoardMainDTO {
    private List<BoardDetailResponseDTO> popularBoards;
    private List<BoardDetailResponseDTO> freeBoards;
    private List<BoardDetailResponseDTO> cookBoards;
}
