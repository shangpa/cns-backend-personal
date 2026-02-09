package com.example.springjwt.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TpReviewSimpleDTO {
    private Long tpReviewId;
    private String postTitle;
    private String content;
    private LocalDateTime createdAt;
}
