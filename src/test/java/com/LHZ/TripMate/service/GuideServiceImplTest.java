package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.entity.GuideMessage;
import com.LHZ.TripMate.entity.GuideSession;
import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.repository.GuideMessageRepository;
import com.LHZ.TripMate.repository.GuideSessionRepository;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import com.LHZ.TripMate.service.impl.GuideServiceImpl;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuideServiceImplTest {

    @Mock GuideSpotConfigRepository spotConfigRepo;
    @Mock GuideSessionRepository sessionRepo;
    @Mock GuideMessageRepository messageRepo;
    @Mock DeepSeekClient deepSeekClient;
    @Mock Executor executor;

    private GuideServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuideServiceImpl(spotConfigRepo, sessionRepo, messageRepo, deepSeekClient, new ObjectMapper());
        service.setExecutor(executor);
    }

    @Test
    void getHistory_returnsEmptyList_whenNoSession() {
        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.empty());

        List<GuideMessageDTO> result = service.getHistory(1L, "swu");

        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_returnsMappedMessages_whenSessionExists() {
        GuideSession session = new GuideSession();
        session.setId(10L);

        GuideMessage msg = new GuideMessage();
        msg.setRole("USER");
        msg.setContent("你好");
        msg.setCreatedAt(LocalDateTime.now());

        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.of(session));
        when(messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(msg));

        List<GuideMessageDTO> result = service.getHistory(1L, "swu");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("USER");
        assertThat(result.get(0).getContent()).isEqualTo("你好");
    }

    @Test
    void clearHistory_deletesMessages_whenSessionExists() {
        GuideSession session = new GuideSession();
        session.setId(42L);
        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.of(session));

        service.clearHistory(1L, "swu");

        verify(messageRepo).deleteBySessionId(42L);
    }

    @Test
    void clearHistory_doesNothing_whenNoSession() {
        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.empty());

        service.clearHistory(1L, "swu");

        verify(messageRepo, never()).deleteBySessionId(any());
    }
}
