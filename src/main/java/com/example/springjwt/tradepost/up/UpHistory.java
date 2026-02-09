package com.example.springjwt.tradepost.up;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "up_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private int userId;            // userentity.id (int)

    @Column(name = "used_points", nullable = false)
    private int usedPoints;        // 500

    @Column(name = "reason", nullable = false, length = 32)
    private String reason;         // "PAID"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
