package com.LHZ.TripMate.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 数据大屏概览：数字人服务运营数据 + 游客行为分析数据 */
@Data
@Builder
public class DashboardOverviewDTO {

    // ---------- 数字人服务（实时业务数据） ----------

    /** 今日游客提问数 */
    private long todayQuestions;
    /** 近7天游客提问数 */
    private long weekQuestions;
    /** 今日活跃会话数 */
    private long todaySessions;
    /** 近7天活跃会话数 */
    private long weekSessions;
    /** 累计服务会话数 */
    private long totalSessions;
    /** 注册用户数 */
    private long totalUsers;
    /** 知识库规模：文档数 + 景点条目数 */
    private long knowledgeCount;

    /** 近14天每日服务趋势 */
    private List<ServicePoint> serviceTrend;
    /** 热门问答 Top10 */
    private List<HotQuestion> hotQuestions;

    // ---------- 游客行为数据（xlsx 导入） ----------

    /** 是否已导入游客行为数据 */
    private boolean visitorDataReady;
    /** 游玩总人次 */
    private long totalVisits;
    /** 平均满意度（1-5） */
    private double avgSatisfaction;
    /** 人均消费 */
    private double avgSpend;

    /** 满意度趋势（按日） */
    private List<SatisfactionPoint> satisfactionTrend;
    /** 满意度 1-5 星分布 */
    private List<DimItem> satisfactionDist;
    /** 景区类型偏好分布 */
    private List<DimItem> attractionTypeDist;
    /** 年龄段分布 */
    private List<DimItem> ageDist;
    /** 性别分布 */
    private List<DimItem> genderDist;

    public record ServicePoint(String date, long questions, long sessions) {}

    public record HotQuestion(String question, long count) {}

    public record SatisfactionPoint(String date, double avgSatisfaction, long visits) {}

    public record DimItem(String label, long value) {}
}
