package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.GuideMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuideMessageRepository extends JpaRepository<GuideMessage, Long> {
    List<GuideMessage> findTop20BySessionIdOrderByCreatedAtDesc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
