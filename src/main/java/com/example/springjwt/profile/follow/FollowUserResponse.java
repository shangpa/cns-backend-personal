package com.example.springjwt.profile.follow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FollowUserResponse {
    private int userId;
    private String name;             // 실명/표시용 이름
    private String username;         // 로그인 아이디(고유)
    private String profileImageUrl;  // 프로필 이미지
    private boolean followingByMe;   // 요청자가 이 유저를 팔로우 중?
    private boolean followingMe;     // 이 유저가 요청자를 팔로우 중?
}