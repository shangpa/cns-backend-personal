package com.example.springjwt.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "userentity")
@Setter
@Getter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String username;
    private String password;

    private String role;
    @Column(nullable = false)
    private int point = 0; // 기본값 0
    @Column(nullable = false)
    private int fridgePointStep = 0;

    @Column(nullable = false)
    private int temperature = 36;

    //위도
    @Column(name = "latitude")
    private Double latitude;

    //경도
    @Column(name = "longitude")
    private Double longitude;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Column(nullable = false)
    private boolean blocked = false;

    private LocalDateTime blockedAt;

    @Column(length = 500)
    private String profileImageUrl;
}