package com.aloute;

import com.aloute.controller.api.AuthController;
import com.aloute.dto.auth.request.RegisterRequestDto;
import com.aloute.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthRegistrationControllerTest {

    @Test
    void registersWithoutIssuingAuthenticationTokenCookie() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.registerUser(new RegisterRequestDto(), response);

        verify(authService).register(org.mockito.ArgumentMatchers.any(RegisterRequestDto.class));
        assertEquals("Account created. Please log in.", result.getBody().getMessage());
        Cookie tokenCookie = response.getCookie("token");
        assertNotNull(tokenCookie);
        assertEquals(0, tokenCookie.getMaxAge());
    }
}
