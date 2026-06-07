package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.badge.BadgeDTO;
import com.LHZ.TripMate.entity.Badge;
import com.LHZ.TripMate.entity.UserBadge;
import com.LHZ.TripMate.repository.BadgeRepository;
import com.LHZ.TripMate.repository.UserBadgeRepository;
import com.LHZ.TripMate.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    public List<BadgeDTO> listAllBadges(String openid) {
        List<Badge> allBadges = badgeRepository.findAllByOrderBySortOrderAsc();

        Map<Long, UserBadge> unlockedMap = openid == null ? Map.of() :
                userBadgeRepository
                .findByOpenidOrderByUnlockedAtDesc(openid)
                .stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, ub -> ub));

        return allBadges.stream()
                .map(badge -> toDTO(badge, unlockedMap.get(badge.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public BadgeDTO unlockBadge(String openid, Long badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "勋章不存在"));

        if (userBadgeRepository.existsByOpenidAndBadgeId(openid, badgeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该勋章已解锁");
        }

        UserBadge userBadge = new UserBadge();
        userBadge.setOpenid(openid);
        userBadge.setBadgeId(badgeId);
        userBadge.setNote("");
        userBadgeRepository.save(userBadge);

        return toDTO(badge, userBadge);
    }

    private BadgeDTO toDTO(Badge badge, UserBadge userBadge) {
        BadgeDTO dto = new BadgeDTO();
        dto.setId(badge.getId());
        dto.setName(badge.getName());
        dto.setDescription(badge.getDescription());
        dto.setType(badge.getType().name());
        dto.setRarity(badge.getRarity().name());
        dto.setIcon(badge.getIcon());
        dto.setUnlockCondition(badge.getUnlockCondition());
        dto.setUnlocked(userBadge != null);
        if (userBadge != null) {
            dto.setUnlockedAt(userBadge.getUnlockedAt());
            dto.setNote(userBadge.getNote());
        }
        return dto;
    }
}
