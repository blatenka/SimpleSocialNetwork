package com.aloute.dto.chat.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.enums.MessageType;
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
public class MessageResponseDto {
    private String id;
    private String conversationId;
    private UserSummaryResponseDto sender;
    private Long recipientId;
    private String content;
    private String mediaUrl;
    private MessageType messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
