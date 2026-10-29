package com.aloute.dto.user.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {
    private Long id;
    private String fullname;
    private String nickname;
    private String avatarUrl;
    private String coverUrl;
    private String description;
    private LocalDate dob;
    private String hometown;
}
