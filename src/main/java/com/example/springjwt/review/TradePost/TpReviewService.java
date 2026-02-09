package com.example.springjwt.review.TradePost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.User.UserRepository;
import com.example.springjwt.tradepost.TradePost;
import com.example.springjwt.tradepost.TradePostRepository;
import com.example.springjwt.notification.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TpReviewService {

    private final TpReviewRepository tpReviewRepository;
    private final TradePostRepository tradePostRepository;
    private final UserRepository userRepository;
    private final FCMService fcmService;

    // 거래글 리뷰 작성
    @Transactional
    public TpReviewResponseDTO createTpReview(TpReviewRequestDTO dto) {
        // 현재 로그인한 사용자
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        // 사용자 엔티티 찾기
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 거래글 엔티티 찾기
        TradePost tradePost = tradePostRepository.findById(dto.getTradePostId())
                .orElseThrow(() -> new IllegalArgumentException("해당 거래글이 없습니다."));

        TpReview tpReview = dto.toEntity(user, tradePost);
        UserEntity postOwner = tradePost.getUser(); // 거래글 작성자
        if (!postOwner.getUsername().equals(username)) {
            fcmService.sendNotificationToUser(
                    postOwner,
                    "거래 리뷰 알림",
                    user.getUsername() + "님이 당신의 거래글에 리뷰를 남겼습니다.",
                    "MATERIAL"
            );
        }
        tpReviewRepository.save(tpReview);

        return TpReviewResponseDTO.fromEntity(tpReview);
    }

    // 특정 거래글의 리뷰 리스트 조회
    @Transactional(readOnly = true)
    public List<TpReviewResponseDTO> getTpReviewsByTradePost(Long tradePostId) {
        List<TpReview> reviews = tpReviewRepository.findByTradePost_TradePostId(tradePostId);
        return reviews.stream()
                .map(TpReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
    // 내가 작성한 리뷰 조회
    @Transactional(readOnly = true)
    public List<TpReviewResponseDTO> getMyTpReviews() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        List<TpReview> myReviews = tpReviewRepository.findByUser_Id(user.getId());

        return myReviews.stream()
                .map(TpReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 내 거래글에 달린 리뷰 조회
    @Transactional(readOnly = true)
    public List<TpReviewResponseDTO> getReviewsOnMyTradePosts() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        List<TpReview> reviewsOnMyPosts = tpReviewRepository.findByTradePost_User_Id(user.getId());

        return reviewsOnMyPosts.stream()
                .map(TpReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TpReviewResponseDTO> getReviewsOnUserTradePostsByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("해당 username을 가진 유저가 존재하지 않습니다.");
        }

        List<TpReview> reviews = tpReviewRepository.findByTradePost_User_Id(user.getId());

        return reviews.stream()
                .map(TpReviewResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }


}