package com.example.springjwt.shorts;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ShortsSearchItem {
    private Long id;
    private String title;
    private String authorName;   // 프론트: authorName
    private String thumbnailUrl; // 프론트: thumbnailUrl
}