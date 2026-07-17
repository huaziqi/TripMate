package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游客行为数据按日聚合：由"景点景区旅游数据行为分析数据"xlsx 导入时流式统计生成，
 * 原始明细不落库，大屏直接读聚合结果。
 */
@Data
@Entity
@Table(name = "visitor_daily_stat")
public class VisitorDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    /** 当日游玩人次 */
    @Column(name = "visit_count", nullable = false)
    private long visitCount;

    /** 当日满意度总和（均值 = satisfactionSum / visitCount） */
    @Column(name = "satisfaction_sum", nullable = false)
    private double satisfactionSum;

    /** 当日消费总和 */
    @Column(name = "spend_sum", nullable = false)
    private double spendSum;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
