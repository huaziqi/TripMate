package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.KnowledgeDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeDocRepository extends JpaRepository<KnowledgeDoc, Long> {

    /** 某景点数字人可用的知识：该景点专属 + 通用（spotKey 为空），启用状态 */
    @Query("""
            SELECT d FROM KnowledgeDoc d
            WHERE d.enabled = true
              AND (d.spotKey = :spotKey OR d.spotKey IS NULL OR d.spotKey = '')
            ORDER BY d.category, d.updatedAt DESC
            """)
    List<KnowledgeDoc> findActiveKnowledgeForSpot(@Param("spotKey") String spotKey);

    /** 管理端列表筛选：spotKey / 分类 / 标题关键字，均可空 */
    @Query("""
            SELECT d FROM KnowledgeDoc d
            WHERE (:spotKey IS NULL OR :spotKey = '' OR d.spotKey = :spotKey)
              AND (:category IS NULL OR d.category = :category)
              AND (:keyword IS NULL OR :keyword = '' OR d.title LIKE CONCAT('%', :keyword, '%'))
            ORDER BY d.updatedAt DESC
            """)
    List<KnowledgeDoc> search(@Param("spotKey") String spotKey,
                              @Param("category") KnowledgeDoc.Category category,
                              @Param("keyword") String keyword);
}
