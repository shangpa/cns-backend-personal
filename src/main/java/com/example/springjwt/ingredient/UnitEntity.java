package com.example.springjwt.ingredient;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "unit",
        uniqueConstraints = @UniqueConstraint(name = "uq_unit_name", columnNames = "name")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 단위 이름 (예: g, kg, ml, L, 개) */
    @Column(length = 20, nullable = false)
    private String name;

    /** 단위 종류 (무게, 부피, 개수) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnitKind kind;
}
