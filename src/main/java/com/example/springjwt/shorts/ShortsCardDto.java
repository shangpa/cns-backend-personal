package com.example.springjwt.shorts;

//레시피 탭
public record ShortsCardDto(Long id, String title, String thumbnailUrl) {
    public static ShortsCardDto from(ShortsVideo v) {
        return new ShortsCardDto(v.getId(), v.getTitle(), v.getThumbnailUrl());
    }
}
