package com.aloute.service.impl;

import com.aloute.dto.auth.request.ForgotPasswordRequestDto;
import com.aloute.dto.auth.request.LoginRequestDto;
import com.aloute.dto.auth.request.RegisterRequestDto;
import com.aloute.dto.auth.request.ResetPasswordRequestDto;
import com.aloute.dto.auth.response.AuthResponseDto;
import com.aloute.exception.BadRequestException;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.model.entity.User;
import com.aloute.model.entity.UserInfo;
import com.aloute.model.enums.Role;
import com.aloute.model.enums.UserStatus;
import com.aloute.repository.mysql.UserRepository;
import com.aloute.security.JwtUtils;
import com.aloute.service.AuthService;
import com.aloute.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final ConcurrentHashMap<String, String> resetTokens = new ConcurrentHashMap<>();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final MailService mailService;
    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Override
    public AuthResponseDto authenticate(LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return authResponse(userDetails);
    }

    @Override
    public void register(RegisterRequestDto request) {
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        UserInfo userInfo = UserInfo.builder()
                .user(user)
                .fullname(request.getFullname())
                .nickname(request.getNickname())
                .dob(request.getDob())
                .hometown(request.getHometown())
                .build();
        user.setUserInfo(userInfo);
        userRepository.save(user);
    }

    @Override
    public void sendPasswordResetLink(ForgotPasswordRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        resetTokens.put(token, request.getEmail());
        String link = UriComponentsBuilder.fromUriString(publicBaseUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
        try {
            mailService.sendPasswordResetEmail(request.getEmail(), link);
        } catch (RuntimeException exception) {
            resetTokens.remove(token);
            throw exception;
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequestDto request) {
        String email = resetTokens.remove(request.getToken());
        if (email == null) {
            throw new BadRequestException("Invalid or expired token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private AuthResponseDto authResponse(UserDetails userDetails) {
        String token = jwtUtils.generateToken(userDetails);
        return AuthResponseDto.builder()
                .token(token)
                .email(userDetails.getUsername())
                .build();
    }
}
