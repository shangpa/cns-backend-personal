package com.example.springjwt.tradepost.request;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.tradepost.TradePost;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
public class TradeCompleteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private TradePost tradePost;

    @ManyToOne
    private UserEntity requester;

    private LocalDateTime requestedAt = LocalDateTime.now();

    public static TradeCompleteRequest of(TradePost post, UserEntity user) {
        TradeCompleteRequest req = new TradeCompleteRequest();
        req.tradePost = post;
        req.requester = user;
        return req;
    }
}
