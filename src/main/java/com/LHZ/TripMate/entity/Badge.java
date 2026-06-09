package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "badge")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeRarity rarity;

    @Column(nullable = false, length = 8)
    private String icon;

    @Column(name = "unlock_condition", nullable = false, length = 200)
    private String unlockCondition;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
