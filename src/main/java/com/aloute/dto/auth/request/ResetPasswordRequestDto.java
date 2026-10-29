package com.aloute.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDto {
    @NotBlank
    @Size(max = 100)
    private String token;

    @NotBlank
    @Size(min = 6)
    private String newPassword;
}
