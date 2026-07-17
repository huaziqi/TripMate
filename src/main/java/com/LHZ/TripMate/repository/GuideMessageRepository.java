package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.GuideMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GuideMessageRepository extends JpaRepository<GuideMessage, Long> {
    List<GuideMessage> findTop20BySessionIdOrderByCreatedAtDesc(Long sessionId);
    void deleteBySessionId(Long sessionId);

    // ---------- 数据大屏统计 ----------

    long countByRoleAndCreatedAtGreaterThanEqual(String role, LocalDateTime from);

    @Query("SELECT COUNT(DISTINCT m.sessionId) FROM GuideMessage m WHERE m.createdAt >= :from")
    long countActiveSessionsSince(@Param("from") LocalDateTime from);

    /** 近 N 天每日提问数与活跃会话数：[date, questions, sessions] */
    @Query(value = """
            SELECT DATE(created_at) AS d,
                   COUNT(*) AS questions,
                   COUNT(DISTINCT session_id) AS sessions
            FROM guide_message
            WHERE role = 'USER' AND created_at >= :from
            GROUP BY DATE(created_at)
            ORDER BY d
            """, nativeQuery = true)
    List<Object[]> dailyServiceCounts(@Param("from") LocalDateTime from);

    /** 热门问答 Top N：[content, count] */
    @Query(value = """
            SELECT content, COUNT(*) AS cnt
            FROM guide_message
            WHERE role = 'USER'
            GROUP BY content
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> hotQuestions(@Param("limit") int limit);
}
