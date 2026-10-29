package com.aloute.dto.user.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserInfoRequestDto {
    @Size(max = 100)
    private String fullname;

    @Size(max = 50)
    private String nickname;

    @Size(max = 2048)
    private String avatarUrl;

    @Size(max = 2048)
    private String coverUrl;

    @Size(max = 2000)
    private String description;

    @PastOrPresent
    private LocalDate dob;

    @Size(max = 100)
    private String hometown;
}
