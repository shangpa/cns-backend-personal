package com.example.springjwt.admin.log;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String adminUsername;         // 누가
    private String action;                // 무엇을 (예: "DELETE_POST")
    private String targetType;            // 대상 유형 (예: "TRADE_POST")
    private Long targetId;                // 대상 ID (예: 게시글 ID)
    private String reason;                // 사유
    private LocalDateTime createdAt;      // 언제

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
