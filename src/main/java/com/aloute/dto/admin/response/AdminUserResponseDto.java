package com.aloute.dto.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponseDto {
    private Long id;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private String fullname;
    private String nickname;
    private String avatarUrl;
}
