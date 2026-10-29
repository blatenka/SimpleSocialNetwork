package com.aloute.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequestDto {
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotBlank
    @Size(min = 6)
    private String confirmPassword;

    @NotBlank
    @Size(max = 100)
    private String fullname;

    @Size(max = 50)
    private String nickname;

    @PastOrPresent
    private LocalDate dob;

    @Size(max = 100)
    private String hometown;
}
