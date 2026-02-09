package com.example.cns.tradepost.request;

import com.example.cns.User.UserEntity;
import com.example.cns.tradepost.TradePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeCompleteRequestRepository extends JpaRepository<TradeCompleteRequest, Long> {

    boolean existsByTradePostAndRequester(TradePost tradePost, UserEntity requester);

    List<TradeCompleteRequest> findByTradePost_TradePostId(Long tradePostId);
}
