package com.aloute.mapper;

import com.aloute.dto.chat.response.MessageResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.document.ChatMessage;

public final class ChatMapper {
    private ChatMapper() {}

    public static MessageResponseDto toResponse(ChatMessage msg, UserSummaryResponseDto sender) {
        if (msg == null) return null;
        return MessageResponseDto.builder()
                .id(msg.getId())
                .conversationId(msg.getConversationId())
                .sender(sender)
                .recipientId(msg.getRecipientId())
                .content(msg.getContent())
                .mediaUrl(msg.getMediaUrl())
                .messageType(msg.getMessageType())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
