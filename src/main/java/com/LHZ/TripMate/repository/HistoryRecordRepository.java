package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.HistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {

    List<HistoryRecord> findTop50ByUserIdOrderByCreateTimeDesc(Long userId);
}