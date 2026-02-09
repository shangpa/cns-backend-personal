package com.example.springjwt.fridge.history;

import com.example.springjwt.User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fridge_history")
@Getter
@Setter
public class FridgeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String ingredientName;

    private Double quantity;

    private String unitDetail;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private LocalDateTime actionDate;

    @Column
    private String dateOption;

    @Column
    private LocalDate fridgeDate;

    @PrePersist
    protected void onCreate() {
        this.actionDate = LocalDateTime.now();
    }

    public enum ActionType {
        ADD, USE
    }
}

