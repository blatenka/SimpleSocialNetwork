package com.aloute.websocket;

import com.aloute.dto.chat.response.MessageResponseDto;
import com.aloute.dto.notification.response.NotificationResponseDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RealtimeEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    private final ObjectMapper objectMapper;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final NotiWebSocketHandler notiWebSocketHandler;

    public RealtimeEventPublisher(ObjectMapper objectMapper,
                                  ChatWebSocketHandler chatWebSocketHandler,
                                  NotiWebSocketHandler notiWebSocketHandler) {
        this.objectMapper = objectMapper;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.notiWebSocketHandler = notiWebSocketHandler;
    }

    public void publishChatMessage(Long userId, MessageResponseDto message) {
        publishChatEvent(userId, Map.of("type", "chat.message", "message", message));
    }

    public void publishUnreadMessageCount(Long userId, Long otherUserId, long count) {
        publishChatEvent(userId, Map.of(
                "type", "chat.unread",
                "userId", otherUserId,
                "count", count));
    }

    public void publishNotification(Long userId, NotificationResponseDto notification, long unreadCount) {
        publishNotificationEvent(userId, Map.of(
                "type", "notification.created",
                "notification", notification,
                "unreadCount", unreadCount));
    }

    public void publishNotificationCount(Long userId, long unreadCount) {
        publishNotificationEvent(userId, Map.of(
                "type", "notification.count",
                "unreadCount", unreadCount));
    }

    private void publishChatEvent(Long userId, Map<String, Object> event) {
        try {
            chatWebSocketHandler.sendToUser(userId, objectMapper.writeValueAsString(event));
        } catch (JacksonException exception) {
            logger.error("Could not serialize chat event for user {}", userId, exception);
        }
    }

    private void publishNotificationEvent(Long userId, Map<String, Object> event) {
        try {
            notiWebSocketHandler.sendToUser(userId, objectMapper.writeValueAsString(event));
        } catch (JacksonException exception) {
            logger.error("Could not serialize notification event for user {}", userId, exception);
        }
    }
}
