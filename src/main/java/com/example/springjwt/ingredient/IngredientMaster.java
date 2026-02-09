package com.example.springjwt.ingredient;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "ingredient_master",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ingredient_name_ko",
                columnNames = {"name_ko"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IngredientMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 한국어 이름 (예: 감자) */
    @Column(name = "name_ko", length = 100, nullable = false)
    private String nameKo;

    /** 재료 카테고리 (enum → DB는 문자열) */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    private IngredientCategory category;

    /** 기본 단위 (예: g, 개 등) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_unit_id", foreignKey = @ForeignKey(name = "fk_ing_unit"))
    private UnitEntity defaultUnit;

    /** 재료 카드 썸네일(일러스트/아이콘) */
    @Column(name = "icon_url", length = 255)
    private String iconUrl;
}
