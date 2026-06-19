package com.LHZ.TripMate.dto.match;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    private String type;
    private Object payload;

    public static WsMessage of(String type) {
        return new WsMessage(type, Map.of());
    }

    public static WsMessage of(String type, Object payload) {
        return new WsMessage(type, payload);
    }
}
