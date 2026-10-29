package com.aloute.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class WebSocketSessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionRegistry.class);

    public enum Channel {
        CHAT,
        NOTIFICATIONS
    }

    private final ConcurrentHashMap<Channel, ConcurrentHashMap<Long, Set<WebSocketSession>>> sessionsByChannel =
            new ConcurrentHashMap<>();

    public WebSocketSessionRegistry() {
        for (Channel channel : Channel.values()) {
            sessionsByChannel.put(channel, new ConcurrentHashMap<>());
        }
    }

    public void add(Channel channel, Long userId, WebSocketSession session) {
        sessionsByChannel.get(channel).compute(userId, (ignored, sessions) -> {
            Set<WebSocketSession> activeSessions = sessions == null ? new CopyOnWriteArraySet<>() : sessions;
            activeSessions.add(session);
            return activeSessions;
        });
    }

    public void remove(Channel channel, Long userId, WebSocketSession session) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByUser = sessionsByChannel.get(channel);
        sessionsByUser.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void sendToUser(Channel channel, Long userId, String payload) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByUser = sessionsByChannel.get(channel);
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                remove(channel, userId, session);
                continue;
            }
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                }
            } catch (IOException exception) {
                logger.warn("Could not deliver WebSocket event to user {}", userId, exception);
                remove(channel, userId, session);
                try {
                    session.close();
                } catch (IOException closeException) {
                    logger.warn("Could not close failed WebSocket session {}", session.getId(), closeException);
                }
            }
        }
    }
}
