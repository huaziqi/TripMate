package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.route.PersonalizeRequestDTO;
import com.LHZ.TripMate.dto.route.PersonalizedRecommendationDTO;
import com.LHZ.TripMate.dto.route.RecommendRouteDTO;
import com.LHZ.TripMate.entity.GuideMessage;
import com.LHZ.TripMate.entity.GuideSession;
import com.LHZ.TripMate.entity.ScenicSpot;
import com.LHZ.TripMate.entity.SpotFavorite;
import com.LHZ.TripMate.repository.*;
import com.LHZ.TripMate.service.impl.RouteRecommendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteRecommendServiceImplTest {

    @Mock ScenicSpotRepository scenicSpotRepository;
    @Mock SpotFavoriteRepository spotFavoriteRepository;
    @Mock HistoryRecordRepository historyRecordRepository;
    @Mock GuideSessionRepository guideSessionRepository;
    @Mock GuideMessageRepository guideMessageRepository;

    private RouteRecommendServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RouteRecommendServiceImpl(
                scenicSpotRepository, spotFavoriteRepository, historyRecordRepository,
                guideSessionRepository, guideMessageRepository);

        // 默认：数据库无景点、无行为数据
        when(scenicSpotRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(List.of());
        when(spotFavoriteRepository.findByUserIdOrderByCreateTimeDesc(anyLong())).thenReturn(List.of());
        when(historyRecordRepository.findTop50ByUserIdOrderByCreateTimeDesc(anyLong())).thenReturn(List.of());
        when(guideSessionRepository.findByUserId(anyLong())).thenReturn(List.of());
    }

    private PersonalizeRequestDTO request(List<String> interests, String duration,
                                          String companions, String stamina) {
        PersonalizeRequestDTO req = new PersonalizeRequestDTO();
        req.setInterests(interests);
        req.setDuration(duration);
        req.setCompanions(companions);
        req.setStamina(stamina);
        return req;
    }

    @Test
    void defaultRoutes_returnsSixLingshanRoutes() {
        List<RecommendRouteDTO> routes = service.defaultRoutes();

        assertThat(routes).hasSize(6);
        assertThat(routes).extracting(RecommendRouteDTO::getId)
                .containsExactlyInAnyOrder("history-deep", "nature-panorama", "family-fun",
                        "art-palace", "blessing-walk", "zen-slow");
    }

    @Test
    void personalize_historyLover_ranksHistoryDeepFirst() {
        PersonalizedRecommendationDTO result = service.personalize(
                request(List.of("history", "buddhist_art"), "full", "solo", "high"), null);

        assertThat(result.getRoutes()).hasSize(6);
        assertThat(result.getRoutes().get(0).getId()).isEqualTo("history-deep");
        assertThat(result.getRoutes().get(0).getMatchScore())
                .isGreaterThan(result.getRoutes().get(2).getMatchScore());
        assertThat(result.getInterestWeights()).containsKeys("历史文化", "佛教艺术");
    }

    @Test
    void personalize_kidsHalfDayLowStamina_ranksFamilyFunFirst() {
        PersonalizedRecommendationDTO result = service.personalize(
                request(List.of("family"), "half", "kids", "low"), null);

        assertThat(result.getRoutes().get(0).getId()).isEqualTo("family-fun");
        assertThat(String.join("", result.getRoutes().get(0).getMatchReasons()))
                .contains("小朋友");
    }

    @Test
    void personalize_noSignals_fallsBackToGeneralRecommendation() {
        PersonalizedRecommendationDTO result = service.personalize(null, null);

        assertThat(result.getRoutes()).hasSize(6);
        assertThat(String.join("", result.getProfileSummary())).contains("通用推荐");
    }

    @Test
    void personalize_favoriteSpots_feedInterestProfile() {
        ScenicSpot temple = new ScenicSpot();
        temple.setId(5L);
        temple.setName("祥符禅寺");

        when(spotFavoriteRepository.findByUserIdOrderByCreateTimeDesc(1L))
                .thenReturn(List.of(new SpotFavorite(1L, 5L)));
        when(scenicSpotRepository.findById(5L)).thenReturn(Optional.of(temple));

        PersonalizedRecommendationDTO result = service.personalize(
                request(List.of(), null, null, null), 1L);

        assertThat(String.join("", result.getProfileSummary())).contains("收藏偏好");
        // 祥符禅寺 → 历史文化权重最高，历史文化深度线应排第一
        assertThat(result.getRoutes().get(0).getId()).isEqualTo("history-deep");
    }

    @Test
    void personalize_chatKeywords_feedInterestProfile() {
        GuideSession session = new GuideSession();
        session.setId(7L);
        session.setUserId(1L);
        session.setSpotKey("lingshan-companion");

        GuideMessage message = new GuideMessage();
        message.setSessionId(7L);
        message.setRole("USER");
        message.setContent("哪里拍照最好看？");

        when(guideSessionRepository.findByUserId(1L)).thenReturn(List.of(session));
        when(guideMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(message));

        PersonalizedRecommendationDTO result = service.personalize(null, 1L);

        assertThat(String.join("", result.getProfileSummary())).contains("对话洞察");
        // 纯摄影画像下，自然风光全景线（摄影权重 0.7）应排第一
        assertThat(result.getRoutes().get(0).getId()).isEqualTo("nature-panorama");
    }

    @Test
    void personalize_matchesSpotsAgainstDatabase() {
        ScenicSpot buddha = new ScenicSpot();
        buddha.setId(11L);
        buddha.setName("灵山大佛");
        buddha.setAddress("灵山胜境景区内");
        buddha.setLatitude(31.0899);
        buddha.setLongitude(120.0937);

        when(scenicSpotRepository.findByNameContainingIgnoreCase("灵山大佛"))
                .thenReturn(List.of(buddha));

        PersonalizedRecommendationDTO result = service.personalize(
                request(List.of("history"), "full", "solo", "high"), null);

        var spots = result.getRoutes().get(0).getSpots();
        var matched = spots.stream().filter(s -> "灵山大佛".equals(s.getName())).findFirst();
        assertThat(matched).isPresent();
        assertThat(matched.get().isMatched()).isTrue();
        assertThat(matched.get().getSpotId()).isEqualTo(11L);
        // 历史画像下，讲解重点应选择历史文化维度
        assertThat(matched.get().getFocusLabel()).isEqualTo("历史文化");
    }
}
