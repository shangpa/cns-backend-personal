// src/main/java/com/example/springjwt/profile/ProfileService.java
package com.example.springjwt.profile;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.profile.follow.Follow;
import com.example.springjwt.profile.follow.FollowRepository;
import com.example.springjwt.profile.follow.FollowUserResponse;
import com.example.springjwt.recipe.RecipeRepository;
import com.example.springjwt.shorts.ShortsVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final ShortsVideoRepository shortsVideoRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public ProfileSummaryResponse getSummary(int targetUserId, int requesterUserId) {
        // 인증 필수이므로 requesterUserId는 null 아님
        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저"));

        long recipeCount = recipeRepository.countByUser_Id(targetUserId);
        long shortsCount = shortsVideoRepository.countByUser_Id(targetUserId);
        long followers   = followRepository.countByFollowing_Id(targetUserId);
        long following   = followRepository.countByFollower_Id(targetUserId);

        boolean mine = (requesterUserId == targetUserId);
        boolean isFollowing = !mine && followRepository
                .existsByFollower_IdAndFollowing_Id(requesterUserId, targetUserId);

        return ProfileSummaryResponse.builder()
                .userId((long) target.getId())   // target.getId()가 int면 캐스팅 유지
                .nickname(target.getUsername())  // 닉네임 필드명에 맞게 필요시 getNickname()으로
                .recipeCount(recipeCount)
                .shortsCount(shortsCount)
                .followersCount(followers)
                .followingCount(following)
                .following(isFollowing)
                .mine(mine)
                .profileImageUrl(target.getProfileImageUrl())
                .build();
    }

    @Transactional
    public boolean toggleFollow(int requesterUserId, int targetUserId) {
        if (requesterUserId == targetUserId) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }
        boolean exists = followRepository.existsByFollower_IdAndFollowing_Id(requesterUserId, targetUserId);
        if (exists) {
            followRepository.deleteByFollower_IdAndFollowing_Id(requesterUserId, targetUserId);
            return false; // 언팔로우됨
        } else {
            var follower  = userRepository.findById(requesterUserId)
                    .orElseThrow(() -> new IllegalArgumentException("요청자 없음"));
            var following = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("대상자 없음"));

            var follow = com.example.springjwt.profile.follow.Follow.builder()
                    .follower(follower)
                    .following(following)
                    .build();
            followRepository.save(follow);
            return true; // 팔로우됨
        }
    }
    public boolean isFollowing(int requesterId, int targetUserId) {
        return followRepository.existsByFollowerIdAndFollowingId(requesterId, targetUserId);
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowings(int targetUserId, int requesterId) {
        // targetUserId가 팔로우하는 사람들 (= targetUserId의 followings)
        List<Follow> rows = followRepository.findByFollower_IdOrderByCreatedAtDesc(targetUserId);

        return rows.stream().map(row -> {
            UserEntity following = row.getFollowing();

            boolean followingByMe = followRepository
                    .existsByFollower_IdAndFollowing_Id(requesterId, following.getId());
            boolean followingMe = followRepository
                    .existsByFollower_IdAndFollowing_Id(following.getId(), requesterId);

            return FollowUserResponse.builder()
                    .userId(following.getId())
                    .name(following.getName())             // ← name
                    .username(following.getUsername())     // ← username
                    .profileImageUrl(following.getProfileImageUrl())
                    .followingByMe(followingByMe)
                    .followingMe(followingMe)
                    .build();
        }).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowers(int targetUserId, int requesterId) {
        // targetUserId의 팔로워들 (누가 나를 팔로우하는가) => following_id = targetUserId
        List<Follow> rows = followRepository.findByFollowing_IdOrderByCreatedAtDesc(targetUserId);

        return rows.stream().map(row -> {
            UserEntity follower = row.getFollower(); // 나를 팔로우한 사람

            boolean followingByMe = followRepository
                    .existsByFollower_IdAndFollowing_Id(requesterId, follower.getId());   // 내가 그를 팔로우?
            boolean followingMe = followRepository
                    .existsByFollower_IdAndFollowing_Id(follower.getId(), requesterId);   // 그가 나를 팔로우?

            return FollowUserResponse.builder()
                    .userId(follower.getId())
                    .name(follower.getName())
                    .username(follower.getUsername())
                    .profileImageUrl(follower.getProfileImageUrl())
                    .followingByMe(followingByMe)
                    .followingMe(followingMe)
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }
}
