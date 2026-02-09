package com.example.springjwt.shorts;

public record ShortsUserVideoDto(
        Long id,
        String title,
        String thumbnailUrl,
        String videoUrl,
        int viewCount,
        int likeCount,
        String createdAt
) {
    public static ShortsUserVideoDto from(ShortsVideo v) {
        return new ShortsUserVideoDto(
                v.getId(),
                v.getTitle(),
                v.getThumbnailUrl(),
                v.getVideoUrl(),
                v.getViewCount(),
                v.getLikeCount(),
                v.getCreatedAt() != null ? v.getCreatedAt().toString() : null
        );
    }

}