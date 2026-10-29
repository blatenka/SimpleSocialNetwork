package com.aloute.service;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.chat.response.MessageResponseDto;
import com.aloute.dto.chat.response.ConversationResponseDto;
import java.util.List;

public interface ChatService {
    MessageResponseDto sendMessage(Long recipientId, String content, String mediaUrl);
    List<ConversationResponseDto> getConversations();
    PageResponseDto<MessageResponseDto> getHistory(Long otherUserId, int page, int size);
    void markAsRead(Long otherUserId);
    long getUnreadCount(Long otherUserId);
}
