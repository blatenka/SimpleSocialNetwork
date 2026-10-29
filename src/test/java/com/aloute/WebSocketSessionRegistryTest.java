package com.aloute;

import com.aloute.websocket.WebSocketSessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketSessionRegistryTest {

    @Test
    void deliversEventsOnlyToTheUsersEndpointSubscription() throws IOException {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        WebSocketSession chat = mock(WebSocketSession.class);
        WebSocketSession notifications = mock(WebSocketSession.class);
        when(chat.isOpen()).thenReturn(true);
        when(notifications.isOpen()).thenReturn(true);
        registry.add(WebSocketSessionRegistry.Channel.CHAT, 12L, chat);
        registry.add(WebSocketSessionRegistry.Channel.NOTIFICATIONS, 12L, notifications);

        registry.sendToUser(WebSocketSessionRegistry.Channel.CHAT, 12L, "{\"type\":\"chat.message\"}");

        verify(chat).sendMessage(new TextMessage("{\"type\":\"chat.message\"}"));
        verify(notifications, never()).sendMessage(new TextMessage("{\"type\":\"chat.message\"}"));
    }

    @Test
    void sendsToEveryOpenTabForTheSameUser() throws IOException {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        WebSocketSession firstTab = mock(WebSocketSession.class);
        WebSocketSession secondTab = mock(WebSocketSession.class);
        when(firstTab.isOpen()).thenReturn(true);
        when(secondTab.isOpen()).thenReturn(true);
        registry.add(WebSocketSessionRegistry.Channel.NOTIFICATIONS, 5L, firstTab);
        registry.add(WebSocketSessionRegistry.Channel.NOTIFICATIONS, 5L, secondTab);

        registry.sendToUser(WebSocketSessionRegistry.Channel.NOTIFICATIONS, 5L, "{\"type\":\"notification.count\"}");

        verify(firstTab).sendMessage(new TextMessage("{\"type\":\"notification.count\"}"));
        verify(secondTab).sendMessage(new TextMessage("{\"type\":\"notification.count\"}"));
    }
}
