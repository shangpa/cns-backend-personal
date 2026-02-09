package com.example.springjwt.profile.follow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollower_IdAndFollowing_Id(int followerId, int followingId);
    long countByFollowing_Id(int followingId); // 팔로워 수
    long countByFollower_Id(int followerId);   // 팔로잉 수
    void deleteByFollower_IdAndFollowing_Id(int followerId, int followingId);
    boolean existsByFollowerIdAndFollowingId(int followerId, int followingId);

    // A가 B를 팔로우한다: follower=A, following=B
    // B의 팔로워들: following_id = B
    List<Follow> findByFollowing_IdOrderByCreatedAtDesc(int followingId);

    // A가 팔로우하는 목록(=A의 팔로잉들): follower_id = A
    List<Follow> findByFollower_IdOrderByCreatedAtDesc(int followerId);

}
