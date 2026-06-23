package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "guide_spot_config")
public class GuideSpotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_key", unique = true, nullable = false, length = 50)
    private String spotKey;

    @Column(name = "persona_name", nullable = false, length = 50)
    private String personaName;

    @Column(name = "persona_desc", columnDefinition = "TEXT")
    private String personaDesc;

    @Column(name = "knowledge_text", columnDefinition = "TEXT")
    private String knowledgeText;

    @Column(nullable = false)
    private boolean active = true;
}
