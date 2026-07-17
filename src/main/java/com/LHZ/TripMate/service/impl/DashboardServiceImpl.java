package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.DashboardDTO;
import com.LHZ.TripMate.dto.admin.DashboardOverviewDTO;
import com.LHZ.TripMate.dto.admin.DashboardOverviewDTO.*;
import com.LHZ.TripMate.entity.VisitorDailyStat;
import com.LHZ.TripMate.entity.VisitorDimStat;
import com.LHZ.TripMate.repository.*;
import com.LHZ.TripMate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AdminUserRepository adminUserRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final GuideMessageRepository guideMessageRepository;
    private final GuideSessionRepository guideSessionRepository;
    private final WxUserRepository wxUserRepository;
    private final KnowledgeDocRepository knowledgeDocRepository;
    private final KnowledgeSpotEntryRepository knowledgeSpotEntryRepository;
    private final VisitorDailyStatRepository visitorDailyStatRepository;
    private final VisitorDimStatRepository visitorDimStatRepository;

    private static final int TREND_DAYS = 14;
    private static final int HOT_QUESTION_LIMIT = 10;

    @Override
    public DashboardDTO getStats() {
        return new DashboardDTO(
                adminUserRepository.count(),
                systemConfigRepository.count()
        );
    }

    @Override
    public DashboardOverviewDTO getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = todayStart.minusDays(6);

        // 服务趋势：近14天，缺失日期补0
        LocalDate trendFrom = LocalDate.now().minusDays(TREND_DAYS - 1);
        Map<LocalDate, ServicePoint> byDate = new HashMap<>();
        for (Object[] r : guideMessageRepository.dailyServiceCounts(trendFrom.atStartOfDay())) {
            LocalDate d = ((Date) r[0]).toLocalDate();
            byDate.put(d, new ServicePoint(d.toString(),
                    ((Number) r[1]).longValue(), ((Number) r[2]).longValue()));
        }
        List<ServicePoint> serviceTrend = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate d = trendFrom.plusDays(i);
            serviceTrend.add(byDate.getOrDefault(d, new ServicePoint(d.toString(), 0, 0)));
        }

        List<HotQuestion> hotQuestions = guideMessageRepository.hotQuestions(HOT_QUESTION_LIMIT).stream()
                .map(r -> new HotQuestion(truncate((String) r[0], 40), ((Number) r[1]).longValue()))
                .toList();

        // 游客行为聚合数据
        List<VisitorDailyStat> dailyStats = visitorDailyStatRepository.findAllByOrderByStatDateAsc();
        long totalVisits = dailyStats.stream().mapToLong(VisitorDailyStat::getVisitCount).sum();
        double satSum = dailyStats.stream().mapToDouble(VisitorDailyStat::getSatisfactionSum).sum();
        double spendSum = dailyStats.stream().mapToDouble(VisitorDailyStat::getSpendSum).sum();

        List<SatisfactionPoint> satisfactionTrend = dailyStats.stream()
                .map(s -> new SatisfactionPoint(
                        s.getStatDate().toString(),
                        round2(s.getSatisfactionSum() / Math.max(1, s.getVisitCount())),
                        s.getVisitCount()))
                .toList();

        return DashboardOverviewDTO.builder()
                .todayQuestions(guideMessageRepository.countByRoleAndCreatedAtGreaterThanEqual("USER", todayStart))
                .weekQuestions(guideMessageRepository.countByRoleAndCreatedAtGreaterThanEqual("USER", weekStart))
                .todaySessions(guideMessageRepository.countActiveSessionsSince(todayStart))
                .weekSessions(guideMessageRepository.countActiveSessionsSince(weekStart))
                .totalSessions(guideSessionRepository.count())
                .totalUsers(wxUserRepository.count())
                .knowledgeCount(knowledgeDocRepository.count() + knowledgeSpotEntryRepository.count())
                .serviceTrend(serviceTrend)
                .hotQuestions(hotQuestions)
                .visitorDataReady(!dailyStats.isEmpty())
                .totalVisits(totalVisits)
                .avgSatisfaction(totalVisits == 0 ? 0 : round2(satSum / totalVisits))
                .avgSpend(totalVisits == 0 ? 0 : round2(spendSum / totalVisits))
                .satisfactionTrend(satisfactionTrend)
                .satisfactionDist(dimItems(VisitorDimStat.Dimension.SATISFACTION, true))
                .attractionTypeDist(dimItems(VisitorDimStat.Dimension.ATTRACTION_TYPE, false))
                .ageDist(dimItems(VisitorDimStat.Dimension.AGE_GROUP, true))
                .genderDist(dimItems(VisitorDimStat.Dimension.GENDER, false))
                .build();
    }

    /** sortByLabel=true 时按标签排序（如 1星..5星、年龄段），否则按数量降序 */
    private List<DimItem> dimItems(VisitorDimStat.Dimension dimension, boolean sortByLabel) {
        List<VisitorDimStat> stats = visitorDimStatRepository.findByDimensionOrderByCountDesc(dimension);
        if (sortByLabel) {
            stats = stats.stream()
                    .sorted(Comparator.comparing(VisitorDimStat::getLabel))
                    .toList();
        }
        return stats.stream().map(s -> new DimItem(s.getLabel(), s.getCount())).toList();
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
