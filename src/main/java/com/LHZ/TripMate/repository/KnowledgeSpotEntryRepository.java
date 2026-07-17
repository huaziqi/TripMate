package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.KnowledgeSpotEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeSpotEntryRepository extends JpaRepository<KnowledgeSpotEntry, Long> {

    Optional<KnowledgeSpotEntry> findBySpotKeyAndSpotCode(String spotKey, String spotCode);

    /** 数字人可用的景点知识条目 */
    List<KnowledgeSpotEntry> findBySpotKeyAndEnabledTrueOrderBySpotCode(String spotKey);

    /** 管理端列表筛选：spotKey / 景区名称 / 关键字（景点ID或名称），均可空 */
    @Query("""
            SELECT e FROM KnowledgeSpotEntry e
            WHERE (:spotKey IS NULL OR :spotKey = '' OR e.spotKey = :spotKey)
              AND (:zoneName IS NULL OR :zoneName = '' OR e.zoneName = :zoneName)
              AND (:keyword IS NULL OR :keyword = ''
                   OR e.spotCode LIKE CONCAT('%', :keyword, '%')
                   OR e.name LIKE CONCAT('%', :keyword, '%'))
            ORDER BY e.spotKey, e.zoneName, e.spotCode
            """)
    List<KnowledgeSpotEntry> search(@Param("spotKey") String spotKey,
                                    @Param("zoneName") String zoneName,
                                    @Param("keyword") String keyword);

    @Query("SELECT DISTINCT e.zoneName FROM KnowledgeSpotEntry e WHERE e.zoneName IS NOT NULL ORDER BY e.zoneName")
    List<String> findDistinctZoneNames();
}
