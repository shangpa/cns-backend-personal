package com.example.springjwt.pantry;

import com.example.springjwt.User.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pantry",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pantry_user_name",
                columnNames = {"user_id", "name"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pantry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_pantry_user"))
    private UserEntity user;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 255)
    private String note;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
