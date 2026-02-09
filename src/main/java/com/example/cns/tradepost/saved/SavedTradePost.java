package com.example.cns.tradepost.saved;

import com.example.cns.User.UserEntity;
import com.example.cns.tradepost.TradePost;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedTradePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    private TradePost tradePost;

    private LocalDateTime savedAt = LocalDateTime.now();
}
