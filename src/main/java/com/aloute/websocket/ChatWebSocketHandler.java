package com.aloute.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final WebSocketSessionRegistry sessionRegistry;

    public ChatWebSocketHandler(WebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long id) {
            sessionRegistry.add(WebSocketSessionRegistry.Channel.CHAT, id, session);
        } else {
            logger.warn("Rejecting chat WebSocket session without an authenticated user");
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (java.io.IOException exception) {
                logger.warn("Could not close unauthenticated chat WebSocket session", exception);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long id) {
            sessionRegistry.remove(WebSocketSessionRegistry.Channel.CHAT, id, session);
        }
    }

    public void sendToUser(Long userId, String payload) {
        sessionRegistry.sendToUser(WebSocketSessionRegistry.Channel.CHAT, userId, payload);
    }
}