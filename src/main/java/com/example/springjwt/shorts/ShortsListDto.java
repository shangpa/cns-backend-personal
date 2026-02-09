package com.example.springjwt.shorts;

public record ShortsListDto(
        Long id,
        String title,
        String videoUrl,
        int viewCount,
        int likeCount,
        Long userId,
        String userName
) {
    public static ShortsListDto from(ShortsVideo v) {
        Long uid = Long.valueOf((v.getUser() != null) ? v.getUser().getId() : null);
        String name = (v.getUser() != null) ? v.getUser().getUsername() : "user";
        return new ShortsListDto(
                v.getId(),
                v.getTitle(),
                v.getVideoUrl(),
                v.getViewCount(),
                v.getLikeCount(),
                uid,
                name
        );
    }
}
