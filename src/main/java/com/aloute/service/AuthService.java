package com.aloute.service;

import com.aloute.dto.auth.request.ForgotPasswordRequestDto;
import com.aloute.dto.auth.request.LoginRequestDto;
import com.aloute.dto.auth.request.RegisterRequestDto;
import com.aloute.dto.auth.request.ResetPasswordRequestDto;
import com.aloute.dto.auth.response.AuthResponseDto;

public interface AuthService {
    AuthResponseDto authenticate(LoginRequestDto request);
    void register(RegisterRequestDto request);
    void sendPasswordResetLink(ForgotPasswordRequestDto request);
    void resetPassword(ResetPasswordRequestDto request);
}
