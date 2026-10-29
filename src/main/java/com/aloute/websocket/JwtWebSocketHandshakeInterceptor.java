package com.aloute.websocket;

import com.aloute.model.entity.User;
import com.aloute.repository.mysql.UserRepository;
import com.aloute.security.JwtUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Component
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public JwtWebSocketHandshakeInterceptor(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String token = Arrays.stream(request.getHeaders().getOrEmpty(HttpHeaders.COOKIE).stream()
                        .flatMap(cookieHeader -> Arrays.stream(cookieHeader.split(";")))
                        .map(String::trim)
                        .filter(cookie -> cookie.startsWith("token="))
                        .map(cookie -> cookie.substring("token=".length()))
                        .toArray(String[]::new))
                .findFirst()
                .orElse(null);
        if (token == null || !jwtUtils.validateJwtToken(token)) {
            return false;
        }

        String email = jwtUtils.getEmailFromJwtToken(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        attributes.put("userId", user.getId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
    }
}
