package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.VisitorDimStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitorDimStatRepository extends JpaRepository<VisitorDimStat, Long> {

    List<VisitorDimStat> findByDimensionOrderByCountDesc(VisitorDimStat.Dimension dimension);
}
