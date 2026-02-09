package com.example.springjwt.point;

import com.example.springjwt.User.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //UserEntity 조인
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private PointActionType action;

    private int pointChange; // +10 또는 -5 등

    private String description; // 예: "레시피 좋아요 10개 돌파"

    private LocalDateTime createdAt;
}
