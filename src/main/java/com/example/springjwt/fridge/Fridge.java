package com.example.springjwt.fridge;

import com.example.springjwt.User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "fridge")
public class Fridge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ingredientName;

    @Column(nullable = false)
    private String storageArea;

    private LocalDate fridgeDate;
    private String dateOption;
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitCategory unitCategory;

    @Column(nullable = false)
    private String unitDetail;

    // UserEntity와의 다대일 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Fridge() {}

    public void setUnitDetail(String unitDetail) {
        if (this.unitCategory != null && !this.unitCategory.isValidDetail(unitDetail)) {
            throw new IllegalArgumentException("유효하지 않은 단위 세부 항목입니다: " + unitDetail);
        }
        this.unitDetail = unitDetail;
    }

    public String getUnitDetail() {
        return unitDetail;
    }


}
