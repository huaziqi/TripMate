package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 游客行为数据维度分布：满意度 / 景区类型 / 年龄段 / 性别 各取值的人次计数
 */
@Data
@Entity
@Table(name = "visitor_dim_stat",
       uniqueConstraints = @UniqueConstraint(columnNames = {"dimension", "label"}))
public class VisitorDimStat {

    public enum Dimension {
        SATISFACTION,     // 满意度 1-5
        ATTRACTION_TYPE,  // 景区类型
        AGE_GROUP,        // 年龄段
        GENDER            // 性别
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Dimension dimension;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(name = "cnt", nullable = false)
    private long count;
}
