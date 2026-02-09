package com.example.cns.notification;

import com.example.cns.User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fcmToken;

    private String platform; // 예: "ANDROID"

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;
}
