// src/main/java/com/example/springjwt/profile/dto/ProfileSummaryResponse.java
package com.example.springjwt.profile;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileSummaryResponse {
    private Long userId;
    private String nickname;
    private long recipeCount;
    private long shortsCount;
    private long followersCount;    // 나를 팔로우하는 사람 수
    private long followingCount;    // 내가 팔로우하는 사람 수
    private boolean following;      // (요청자 -> 대상) 팔로잉 여부
    private boolean mine;           // 내 프로필인지
    private String profileImageUrl; //프로필 이미지
}
