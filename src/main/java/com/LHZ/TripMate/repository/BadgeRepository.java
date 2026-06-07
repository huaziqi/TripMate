package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.Badge;
import com.LHZ.TripMate.entity.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    List<Badge> findByTypeOrderBySortOrderAsc(BadgeType type);
    List<Badge> findAllByOrderBySortOrderAsc();
}
