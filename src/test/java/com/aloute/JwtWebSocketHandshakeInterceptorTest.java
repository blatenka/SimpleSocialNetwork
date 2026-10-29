package com.aloute;

import com.aloute.model.entity.User;
import com.aloute.repository.mysql.UserRepository;
import com.aloute.security.JwtUtils;
import com.aloute.websocket.JwtWebSocketHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtWebSocketHandshakeInterceptorTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtWebSocketHandshakeInterceptor interceptor =
            new JwtWebSocketHandshakeInterceptor(jwtUtils, userRepository);

    @Test
    void acceptsValidTokenCookieAndStoresResolvedUserId() {
        when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtils.getEmailFromJwtToken("valid-token")).thenReturn("person@example.test");
        when(userRepository.findByEmail("person@example.test")).thenReturn(
                Optional.of(User.builder().id(34L).email("person@example.test").build()));
        var attributes = new HashMap<String, Object>();
        ServerHttpRequest request = requestWithCookie("token=valid-token");

        boolean accepted = interceptor.beforeHandshake(
                request, mock(ServerHttpResponse.class), null, attributes);

        assertTrue(accepted);
        assertEquals(34L, attributes.get("userId"));
    }

    @Test
    void rejectsMissingOrInvalidToken() {
        ServerHttpRequest missingCookieRequest = mock(ServerHttpRequest.class);
        when(missingCookieRequest.getHeaders()).thenReturn(new HttpHeaders());
        assertFalse(interceptor.beforeHandshake(
                missingCookieRequest, mock(ServerHttpResponse.class), null, new HashMap<>()));
        when(jwtUtils.validateJwtToken("expired-token")).thenReturn(false);
        assertFalse(interceptor.beforeHandshake(
                requestWithCookie("token=expired-token"), mock(ServerHttpResponse.class), null, new HashMap<>()));
    }

    private ServerHttpRequest requestWithCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }
}
