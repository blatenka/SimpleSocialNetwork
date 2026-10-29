package com.aloute.dto.chat.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
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
public class ConversationResponseDto {
    private UserSummaryResponseDto user;
    private MessageResponseDto latestMessage;
    private long unreadCount;
}
