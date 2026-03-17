package com.example.cns.board;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class BoardRequestDTO {
    @NotBlank(message = "내용은 필수입니다")
    private String content;
    private List<String> imageUrls;
    private BoardType boardType;
}
