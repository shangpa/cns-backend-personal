package com.example.springjwt.review.TradePost;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.admin.dto.TpReviewSimpleDTO;
import com.example.springjwt.tradepost.TradePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TpReviewRepository extends JpaRepository<TpReview, Long> {
    List<TpReview> findByTradePost_TradePostId(Long tradePostId); // 특정 거래글 리뷰 조회

    List<TpReview> findByUser_Id(int userId);
    List<TpReview> findByTradePost_User_Id(int userId);

    void deleteAllByTradePost(TradePost post);
    // 1. 내가 작성한 후기 조회
    @Query("SELECT new com.example.springjwt.admin.dto.TpReviewSimpleDTO(" +
            "r.tpReviewId, r.tradePost.title, r.content, r.createdAt) " +
            "FROM TpReview r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<TpReviewSimpleDTO> findReviewsWrittenByUser(@Param("userId") int userId);

    // 2. 내가 받은 후기 조회 (내 거래글에 달린 후기)
    @Query("SELECT new com.example.springjwt.admin.dto.TpReviewSimpleDTO(" +
            "r.tpReviewId, r.tradePost.title, r.content, r.createdAt) " +
            "FROM TpReview r WHERE r.tradePost.user.id = :userId ORDER BY r.createdAt DESC")
    List<TpReviewSimpleDTO> findReviewsReceivedByUser(@Param("userId") int userId);

    int countByUser(UserEntity user);

    @Query("SELECT AVG(r.rating) FROM TpReview r WHERE r.user.id = :userId")
    Double avgRatingByUser(Long userId);
}