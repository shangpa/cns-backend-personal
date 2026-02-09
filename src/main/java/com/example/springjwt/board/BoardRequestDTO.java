package com.example.springjwt.board;

import lombok.Getter;

import java.util.List;

@Getter
public class BoardRequestDTO {
    private String content;
    private List<String> imageUrls;
    private BoardType boardType;
}
