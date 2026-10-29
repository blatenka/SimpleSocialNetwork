package com.aloute.dto.admin.request;

import com.aloute.model.enums.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRequestDto {
    @Size(max = 100)
    private String fullname;

    @Size(max = 50)
    private String nickname;

    private UserStatus status;
}
