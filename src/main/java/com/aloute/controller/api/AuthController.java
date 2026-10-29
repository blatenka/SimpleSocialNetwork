package com.aloute.controller.api;

import com.aloute.dto.common.ApiMessageResponseDto;
import com.aloute.dto.auth.request.ForgotPasswordRequestDto;
import com.aloute.dto.auth.request.LoginRequestDto;
import com.aloute.dto.auth.request.RegisterRequestDto;
import com.aloute.dto.auth.request.ResetPasswordRequestDto;
import com.aloute.dto.auth.response.AuthResponseDto;
import com.aloute.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);
    }

    private void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> authenticateUser(
            @Valid @RequestBody LoginRequestDto loginRequest, HttpServletResponse response) {
        AuthResponseDto auth = authService.authenticate(loginRequest);
        setTokenCookie(response, auth.getToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiMessageResponseDto> registerUser(
            @Valid @RequestBody RegisterRequestDto request, HttpServletResponse response) {
        authService.register(request);
        clearTokenCookie(response);
        return ResponseEntity.ok(ApiMessageResponseDto.builder()
                .message("Account created. Please log in.")
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponseDto> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Logged out").build());
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.sendPasswordResetLink(request);
        return ResponseEntity.ok(ApiMessageResponseDto.builder()
                .message("If the email exists, a reset link has been sent.")
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Password reset successfully").build());
    }
}
