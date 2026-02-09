package com.example.springjwt.pantry;

import com.example.springjwt.ingredient.IngredientMaster;
import com.example.springjwt.ingredient.UnitEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pantry_stock",
        indexes = {
                @Index(name = "ix_stock_pantry", columnList = "pantry_id"),
                @Index(name = "ix_stock_pantry_ing", columnList = "pantry_id, ingredient_id"),
                @Index(name = "ix_stock_expires", columnList = "expiresAt"),
                @Index(name = "ix_stock_storage", columnList = "storage"),                    // 스토리지 필터
                @Index(name = "ix_stock_pantry_storage", columnList = "pantry_id, storage"),  // 도넛 통계
                @Index(name = "ix_stock_fifo", columnList = "pantry_id, ingredient_id, expiresAt, created_at") // FIFO 차감
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PantryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pantry_id", foreignKey = @ForeignKey(name = "fk_stock_pantry"))
    private Pantry pantry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", foreignKey = @ForeignKey(name = "fk_stock_ingredient"))
    private IngredientMaster ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", foreignKey = @ForeignKey(name = "fk_stock_unit"))
    private UnitEntity unit;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StorageLocation storage = StorageLocation.FRIDGE;

    private LocalDate purchasedAt;
    private LocalDate expiresAt;

    @Column(length = 255)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private StockSource source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
