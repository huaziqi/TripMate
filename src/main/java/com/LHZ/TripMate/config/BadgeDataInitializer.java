package com.LHZ.TripMate.config;

import com.LHZ.TripMate.entity.Badge;
import com.LHZ.TripMate.entity.BadgeRarity;
import com.LHZ.TripMate.entity.BadgeType;
import com.LHZ.TripMate.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BadgeDataInitializer {

    private final BadgeRepository badgeRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (badgeRepository.count() > 0) return;

        List<Badge> badges = List.of(
            badge("初探故宫",   "踏入这座六百年的皇家宫殿",  BadgeType.SPOT, BadgeRarity.RARE,      "🏯", "扫描故宫景点二维码",   1),
            badge("长城守望者", "登上万里长城，俯瞰山河",    BadgeType.SPOT, BadgeRarity.EPIC,      "🧱", "扫描长城景点二维码",   2),
            badge("西湖漫步",   "走过断桥，赏过荷花",        BadgeType.SPOT, BadgeRarity.COMMON,    "🏞", "扫描西湖景点二维码",   3),
            badge("黄山揽胜",   "云海、奇松、怪石、温泉",    BadgeType.SPOT, BadgeRarity.EPIC,      "⛰",  "扫描黄山景点二维码",   4),
            badge("外滩夜游",   "万国建筑群在霓虹中闪耀",    BadgeType.SPOT, BadgeRarity.RARE,      "🌃", "扫描外滩景点二维码",   5),
            badge("兵马俑见证", "与沉睡两千年的军阵相遇",    BadgeType.SPOT, BadgeRarity.LEGENDARY, "🏺", "扫描兵马俑景点二维码", 6),
            badge("旅行起点",   "开启你的第一次打卡",        BadgeType.ACHIEVEMENT, BadgeRarity.COMMON,    "🌟", "完成第一次景点打卡",     10),
            badge("探索者",     "已打卡 5 个不同景点",       BadgeType.ACHIEVEMENT, BadgeRarity.RARE,      "🧭", "累计打卡 5 个景点",      11),
            badge("旅行达人",   "已打卡 20 个不同景点",      BadgeType.ACHIEVEMENT, BadgeRarity.EPIC,      "✈", "累计打卡 20 个景点",     12),
            badge("传奇旅人",   "已打卡 50 个不同景点",      BadgeType.ACHIEVEMENT, BadgeRarity.LEGENDARY, "🌏", "累计打卡 50 个景点",     13),
            badge("夜行者",     "在晚上 10 点后完成打卡",    BadgeType.ACHIEVEMENT, BadgeRarity.RARE,      "🦉", "晚上 22:00 后完成打卡", 14),
            badge("城市征服者", "在同一城市打卡 3 个景点",   BadgeType.ACHIEVEMENT, BadgeRarity.EPIC,      "🏙", "同城市打卡 3 个景点",   15)
        );

        badgeRepository.saveAll(badges);
    }

    private Badge badge(String name, String desc, BadgeType type, BadgeRarity rarity,
                        String icon, String condition, int order) {
        Badge b = new Badge();
        b.setName(name);
        b.setDescription(desc);
        b.setType(type);
        b.setRarity(rarity);
        b.setIcon(icon);
        b.setUnlockCondition(condition);
        b.setSortOrder(order);
        return b;
    }
}
