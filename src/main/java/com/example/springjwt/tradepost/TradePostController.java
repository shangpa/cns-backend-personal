package com.example.springjwt.tradepost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.User.UserService;
import com.example.springjwt.dto.CustomUserDetails;
import com.example.springjwt.tradepost.request.TradeCompleteRequest;
import com.example.springjwt.tradepost.request.TradeCompleteRequestRepository;
import com.example.springjwt.tradepost.request.TradeCompleteRequestService;
import com.example.springjwt.tradepost.request.UserSimpleDTO;
import com.example.springjwt.tradepost.up.TradePostUpResult;
import com.example.springjwt.tradepost.up.TradePostUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade-posts")
@RequiredArgsConstructor
public class TradePostController {

    private final TradePostService tradePostService;
    private final UserService userService;
    private final TradeCompleteRequestService tradeCompleteRequestService;
    private final TradeCompleteRequestRepository tradeCompleteRequestRepository;
    private final UserRepository userRepository;
    private final TradePostUpService tradePostUpService;

    // 거래글 생성
    @PostMapping
    public ResponseEntity<TradePostDTO> createTradePost(
            @RequestBody TradePostDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TradePost post = tradePostService.create(dto, userDetails.getUsername());
        return ResponseEntity.ok(TradePostDTO.fromEntity(post));
    }

    // 거래글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<TradePostDTO> getTradePostById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String username = (userDetails != null) ? userDetails.getUsername() : null;
        TradePostDTO dto = tradePostService.getTradePostById(id, username);
        return ResponseEntity.ok(dto);
    }

    // 내가 작성한 거래글
    @GetMapping("/my-posts")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getMyTradePosts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<TradePostSimpleResponseDTO> myPosts =
                tradePostService.getMyTradePosts(userDetails.getUsername());
        return ResponseEntity.ok(myPosts);
    }

    // 전체 거래글
    @GetMapping
    public ResponseEntity<List<TradePostDTO>> getAllTradePosts() {
        List<TradePostDTO> tradePosts = tradePostService.getAllTradePosts();
        return ResponseEntity.ok(tradePosts);
    }

    // 단일 카테고리 필터 (레거시) - 내부에서 통합 메서드로 위임
    @GetMapping("/category")
    public ResponseEntity<List<TradePostDTO>> getTradePostsByCategory(
            @RequestParam("category") String category
    ) {
        // 카테고리만 적용
        List<TradePostDTO> tradePosts =
                tradePostService.getNearbyFlexible(null, null, List.of(category), "LATEST");
        return ResponseEntity.ok(tradePosts);
    }

    // 키워드 검색
    @GetMapping("/search")
    public ResponseEntity<List<TradePostDTO>> searchTradePosts(
            @RequestParam("keyword") String keyword
    ) {
        List<TradePostDTO> result = tradePostService.searchTradePosts(keyword);
        return ResponseEntity.ok(result);
    }

    // ✅ 통합 엔드포인트: 거리/카테고리 모두 선택적
    // 예) /nearby?categories=COOKWARE&categories=TABLEWARE
    //     /nearby?distanceKm=1.5
    //     /nearby?distanceKm=2.0&categories=COOKWARE
    //     /nearby
    @GetMapping("/nearby")
    public ResponseEntity<List<TradePostDTO>> getNearbyFlexible(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Double distanceKm,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false, defaultValue = "LATEST") String sort
    ) {
        String username = (userDetails != null) ? userDetails.getUsername() : null;
        return ResponseEntity.ok(
                tradePostService.getNearbyFlexible(username, distanceKm, categories, sort)
        );
    }

    // 거리순 (레거시) - 통합 메서드 위임
    @GetMapping("/sorted-by-distance")
    public ResponseEntity<List<TradePostDTO>> getSortedByDistance(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String username = (userDetails != null) ? userDetails.getUsername() : null;
        // 정렬만 거리순으로 처리하고 나머지 조건은 없음
        return ResponseEntity.ok(
                tradePostService.getNearbyFlexible(username, null, null, "DISTANCE")
        );
    }

    // 카테고리 + 거리 (레거시) - 통합 메서드 위임
    @GetMapping("/nearby/filter")
    public ResponseEntity<List<TradePostDTO>> getNearbyByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam double distanceKm,
            @RequestParam String category
    ) {
        String username = (userDetails != null) ? userDetails.getUsername() : null;
        return ResponseEntity.ok(
                tradePostService.getNearbyFlexible(username, distanceKm, List.of(category), "LATEST")
        );
    }

    // 다중 카테고리 + 거리 (레거시) - 통합 메서드 위임
    @GetMapping("/nearby-by-multiple-categories")
    public ResponseEntity<List<TradePostDTO>> getNearbyPostsByMultipleCategories(
            @RequestParam double distanceKm,
            @RequestParam List<String> categories,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String username = (userDetails != null) ? userDetails.getUsername() : null;
        return ResponseEntity.ok(
                tradePostService.getNearbyFlexible(username, distanceKm, categories, "LATEST")
        );
    }

    // 인기 거래글
    @GetMapping("/popular")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getPopularTradePosts() {
        List<TradePostSimpleResponseDTO> topPosts = tradePostService.getTop3PopularTradePosts();
        return ResponseEntity.ok(topPosts);
    }

    // 조회수 증가
    @PatchMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        tradePostService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    // 거래 완료 요청
    @PostMapping("/{postId}/complete-request")
    public ResponseEntity<String> requestComplete(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return tradeCompleteRequestService.createRequest(postId, userDetails.getUsername());
    }

    // 거래 완료 요청자 목록
    @GetMapping("/{id}/complete-requests")
    public ResponseEntity<List<UserSimpleDTO>> getRequesters(@PathVariable Long id) {
        List<TradeCompleteRequest> list =
                tradeCompleteRequestRepository.findByTradePost_TradePostId(id);
        return ResponseEntity.ok(
                list.stream().map(req -> UserSimpleDTO.fromEntity(req.getRequester())).toList()
        );
    }

    // 내가 구매한 거래글
    @GetMapping("/mypurchases")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getMyPurchasedPosts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<TradePostSimpleResponseDTO> purchases =
                tradePostService.getMyPurchasedPosts(userDetails.getUsername());
        return ResponseEntity.ok(purchases);
    }

    // 거래 완료 처리
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TradePostDTO> completeTradePost(
            @PathVariable Long id,
            @RequestParam Long buyerId
    ) {
        TradePost completedPost = tradePostService.completeTradePost(id, buyerId);
        return ResponseEntity.ok(TradePostDTO.fromEntity(completedPost));
    }

    // 특정 유저의 판매글
    @GetMapping("/user/{username}")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getPostsByUsername(
            @PathVariable String username
    ) {
        return ResponseEntity.ok(tradePostService.getPostsByUsername(username));
    }

    // 특정 유저의 판매글(상태별)
    @GetMapping("/user/{username}/status/{status}")
    public ResponseEntity<List<TradePostSimpleResponseDTO>> getPostsByUsernameAndStatus(
            @PathVariable String username,
            @PathVariable int status
    ) {
        return ResponseEntity.ok(tradePostService.getPostsByUsernameAndStatus(username, status));
    }

    // 프로필 요약
    @GetMapping("/info")
    public ResponseEntity<UserProfileResponseDTO> getUserProfileInfo(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = (userDetails != null) ? userDetails.getUsername() : null;
        if (username == null) {
            return ResponseEntity.badRequest().build();
        }
        UserEntity user = userRepository.findByUsername(username);
        return ResponseEntity.ok(tradePostService.getUserProfile(user));
    }

    // 끌어올리기
    @PostMapping("/{id}/up")
    public ResponseEntity<TradePostUpResult> up(
            @PathVariable Long id,
            @AuthenticationPrincipal com.example.springjwt.dto.CustomUserDetails userDetails
    ) {
        var result = tradePostUpService.up(id, userDetails.getUserEntity());
        return ResponseEntity.ok(result);
    }
}
