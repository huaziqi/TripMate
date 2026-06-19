package com.LHZ.TripMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService();
    }

    private WebSocketSession mockSession(String id) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void firstUserJoins_getsEmptyPartner() {
        WebSocketSession s1 = mockSession("s1");
        Optional<WebSocketSession> result = matchService.tryMatch(s1, 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void secondUserJoins_sameSpot_bothGetMatched() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        Optional<WebSocketSession> result = matchService.tryMatch(s2, 1L);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("s1");
    }

    @Test
    void secondUserJoins_differentSpot_noMatch() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        Optional<WebSocketSession> result = matchService.tryMatch(s2, 2L);
        assertThat(result).isEmpty();
    }

    @Test
    void getPartner_afterMatch_returnsPartner() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.tryMatch(s2, 1L);
        assertThat(matchService.getPartner("s1")).isPresent();
        assertThat(matchService.getPartner("s2")).isPresent();
    }

    @Test
    void confirm_onlyOneConfirmed_returnsFalse() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.tryMatch(s2, 1L);
        assertThat(matchService.confirm("s1")).isFalse();
    }

    @Test
    void confirm_bothConfirmed_returnsTrue() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.tryMatch(s2, 1L);
        matchService.confirm("s1");
        assertThat(matchService.confirm("s2")).isTrue();
    }

    @Test
    void cancel_removesFromQueue() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.cancel(s1);
        Optional<WebSocketSession> result = matchService.tryMatch(s2, 1L);
        assertThat(result).isEmpty();
    }
}
