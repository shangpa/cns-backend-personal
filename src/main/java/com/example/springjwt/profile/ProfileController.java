package com.example.springjwt.profile;

import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.profile.follow.FollowUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}/summary")
    public ResponseEntity<ProfileSummaryResponse> getSummary(
            @PathVariable int userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int requesterId = userDetails.getUserEntity().getId(); // int 가정
        return ResponseEntity.ok(profileService.getSummary(userId, requesterId));
    }

    @PostMapping("/{userId}/follow-toggle")
    public ResponseEntity<Void> toggleFollow(
            @PathVariable int userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int requesterId = userDetails.getUserEntity().getId();
        profileService.toggleFollow(requesterId, userId);
        return ResponseEntity.ok().build(); // 본문 없음
    }

    @GetMapping("/{userId}/is-following")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable int userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int requesterId = userDetails.getUserEntity().getId();
        boolean following = profileService.isFollowing(requesterId, userId);
        return ResponseEntity.ok(following);
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<FollowUserResponse>> getFollowers(
            @PathVariable int userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int requesterId = userDetails.getUserEntity().getId();
        return ResponseEntity.ok(profileService.getFollowers(userId, requesterId));
    }

    @GetMapping("/{userId}/followings")
    public ResponseEntity<List<FollowUserResponse>> getFollowings(
            @PathVariable int userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int requesterId = userDetails.getUserEntity().getId();
        return ResponseEntity.ok(profileService.getFollowings(userId, requesterId));
    }
}
