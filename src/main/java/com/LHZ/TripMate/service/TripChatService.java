package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.TripChatRequestDTO;
import com.LHZ.TripMate.dto.TripChatResponseDTO;

public interface TripChatService {
    TripChatResponseDTO chat(TripChatRequestDTO request);
}
