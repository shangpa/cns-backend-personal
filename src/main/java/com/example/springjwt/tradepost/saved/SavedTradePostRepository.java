package com.example.springjwt.tradepost.saved;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.tradepost.TradePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedTradePostRepository extends JpaRepository<SavedTradePost, Long> {
    Optional<SavedTradePost> findByUserAndTradePost(UserEntity user, TradePost tradePost);
    List<SavedTradePost> findByUser(UserEntity user);
    void deleteByUserAndTradePost(UserEntity user, TradePost tradePost);
    int countByTradePost(TradePost tradePost);
    void deleteAllByTradePost(TradePost post);
}
