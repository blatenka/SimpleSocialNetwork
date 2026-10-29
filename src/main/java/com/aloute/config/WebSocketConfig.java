package com.aloute.config;

import com.aloute.websocket.ChatWebSocketHandler;
import com.aloute.websocket.JwtWebSocketHandshakeInterceptor;
import com.aloute.websocket.NotiWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final NotiWebSocketHandler notiWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           NotiWebSocketHandler notiWebSocketHandler,
                           JwtWebSocketHandshakeInterceptor jwtHandshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.notiWebSocketHandler = notiWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(jwtHandshakeInterceptor);
        registry.addHandler(notiWebSocketHandler, "/ws/notifications")
                .addInterceptors(jwtHandshakeInterceptor);
    }
}