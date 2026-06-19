package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.dto.match.WsMessage;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.service.MatchService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MatchWebSocketHandler extends TextWebSocketHandler {

    private final MatchService matchService;
    private final ObjectMapper objectMapper;
    private final WxUserRepository wxUserRepository;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String openid = (String) session.getAttributes().get("openid");
        sessions.put(session.getId(), session);
        matchService.registerSession(session, openid);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WsMessage msg = objectMapper.readValue(message.getPayload(), WsMessage.class);
        switch (msg.getType()) {
            case "join"     -> handleJoin(session, (Map<String, Object>) msg.getPayload());
            case "confirm"  -> handleConfirm(session);
            case "cancel"   -> handleCancel(session);
            case "location" -> handleLocation(session, (Map<String, Object>) msg.getPayload());
            case "leave"    -> handleLeave(session);
        }
    }

    private void handleJoin(WebSocketSession session, Map<String, Object> payload) throws Exception {
        Long spotId = Long.valueOf(payload.get("spotId").toString());
        String spotName = payload.getOrDefault("spotName", "未知景点").toString();

        Optional<WebSocketSession> partnerOpt = matchService.tryMatch(session, spotId);

        if (partnerOpt.isEmpty()) {
            send(session, WsMessage.of("waiting"));
        } else {
            WebSocketSession partner = partnerOpt.get();
            String myNickname = getNickname(matchService.getOpenid(session.getId()));
            String partnerNickname = getNickname(matchService.getOpenid(partner.getId()));

            send(session, WsMessage.of("matched",
                    Map.of("partnerNickname", partnerNickname, "spotName", spotName)));
            send(partner, WsMessage.of("matched",
                    Map.of("partnerNickname", myNickname, "spotName", spotName)));
        }
    }

    private void handleConfirm(WebSocketSession session) throws Exception {
        boolean bothConfirmed = matchService.confirm(session.getId());
        if (bothConfirmed) {
            send(session, WsMessage.of("confirmed"));
            matchService.getPartner(session.getId())
                    .ifPresent(p -> sendSilently(p, WsMessage.of("confirmed")));
        }
    }

    private void handleCancel(WebSocketSession session) throws Exception {
        Optional<WebSocketSession> partnerOpt = matchService.getPartner(session.getId());
        matchService.cancel(session);
        partnerOpt.ifPresent(p -> sendSilently(p, WsMessage.of("partnerCancelled")));
    }

    private void handleLocation(WebSocketSession session, Map<String, Object> payload) {
        matchService.getPartner(session.getId())
                .ifPresent(p -> sendSilently(p, WsMessage.of("locationUpdate", payload)));
    }

    private void handleLeave(WebSocketSession session) {
        Optional<WebSocketSession> partnerOpt = matchService.getPartner(session.getId());
        matchService.cancel(session);
        partnerOpt.ifPresent(p -> sendSilently(p, WsMessage.of("partnerLeft")));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Optional<WebSocketSession> partnerOpt = matchService.getPartner(session.getId());
        sessions.remove(session.getId());
        matchService.removeSession(session);
        partnerOpt.ifPresent(p -> sendSilently(p, WsMessage.of("partnerLeft")));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Optional<WebSocketSession> partnerOpt = matchService.getPartner(session.getId());
        sessions.remove(session.getId());
        matchService.removeSession(session);
        partnerOpt.ifPresent(p -> sendSilently(p, WsMessage.of("partnerLeft")));
    }

    private void send(WebSocketSession session, WsMessage msg) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    private void sendSilently(WebSocketSession session, WsMessage msg) {
        try { send(session, msg); } catch (Exception ignored) {}
    }

    private String getNickname(String openid) {
        return wxUserRepository.findByOpenid(openid)
                .map(u -> u.getNickname() != null && !u.getNickname().isBlank()
                        ? u.getNickname() : "旅行者")
                .orElse("旅行者");
    }
}
