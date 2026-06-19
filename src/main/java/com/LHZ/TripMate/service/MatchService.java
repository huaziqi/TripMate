package com.LHZ.TripMate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MatchService {

    private final ConcurrentHashMap<Long, WebSocketSession> waitingQueue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> matchedPairs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> confirmMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionOpenid = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session, String openid) {
        sessionOpenid.put(session.getId(), openid);
    }

    public Optional<WebSocketSession> tryMatch(WebSocketSession session, Long spotId) {
        AtomicReference<WebSocketSession> partnerRef = new AtomicReference<>();
        waitingQueue.compute(spotId, (key, existing) -> {
            if (existing == null || existing.getId().equals(session.getId())) {
                return session;
            }
            partnerRef.set(existing);
            return null;
        });

        WebSocketSession partner = partnerRef.get();
        if (partner != null) {
            matchedPairs.put(session.getId(), partner);
            matchedPairs.put(partner.getId(), session);
        }
        return Optional.ofNullable(partner);
    }

    public Optional<WebSocketSession> getPartner(String sessionId) {
        return Optional.ofNullable(matchedPairs.get(sessionId));
    }

    public boolean confirm(String sessionId) {
        confirmMap.put(sessionId, true);
        WebSocketSession partner = matchedPairs.get(sessionId);
        return partner != null && Boolean.TRUE.equals(confirmMap.get(partner.getId()));
    }

    public void cancel(WebSocketSession session) {
        String sessionId = session.getId();
        confirmMap.remove(sessionId);
        waitingQueue.values().remove(session);
        WebSocketSession partner = matchedPairs.remove(sessionId);
        if (partner != null) {
            matchedPairs.remove(partner.getId());
            confirmMap.remove(partner.getId());
        }
    }

    public void removeSession(WebSocketSession session) {
        cancel(session);
        sessionOpenid.remove(session.getId());
    }

    public String getOpenid(String sessionId) {
        return sessionOpenid.getOrDefault(sessionId, "");
    }
}
