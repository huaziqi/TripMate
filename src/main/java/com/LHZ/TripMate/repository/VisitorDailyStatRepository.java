package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.VisitorDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitorDailyStatRepository extends JpaRepository<VisitorDailyStat, Long> {

    List<VisitorDailyStat> findAllByOrderByStatDateAsc();
}
