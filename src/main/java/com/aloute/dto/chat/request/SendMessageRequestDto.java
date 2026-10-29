package com.aloute.dto.chat.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequestDto {
    @NotNull
    @Positive
    private Long recipientId;

    @Size(max = 5000)
    private String content;

    @Size(max = 2048)
    private String mediaUrl;
}
