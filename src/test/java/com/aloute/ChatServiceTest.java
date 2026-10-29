package com.aloute;

import com.aloute.model.document.ChatMessage;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.service.impl.ChatServiceImpl;
import com.aloute.service.NotificationService;
import com.aloute.service.UserService;
import com.aloute.repository.mongo.ChatMessageRepository;
import com.aloute.util.SecurityUtils;
import com.aloute.websocket.RealtimeEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final UserService userService = mock(UserService.class);
    private final ChatServiceImpl chatService = new ChatServiceImpl(
            messages, mongoTemplate, securityUtils, mock(NotificationService.class), userService,
            mock(RealtimeEventPublisher.class));

    @Test
    void listsOtherParticipantsWithTheirLatestMessage() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 29, 12, 0);
        ChatMessage latestMessage = ChatMessage.builder()
                .id("message-1")
                .conversationId("1_2")
                .senderId(2L)
                .recipientId(1L)
                .content("Hello")
                .createdAt(createdAt)
                .isDeleted(false)
                .build();
        UserSummaryResponseDto otherUser = UserSummaryResponseDto.builder()
                .id(2L)
                .fullname("Other User")
                .build();
        UserSummaryResponseDto sender = UserSummaryResponseDto.builder()
                .id(2L)
                .fullname("Other User")
                .build();
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(mongoTemplate.findDistinct(any(Query.class), eq("conversationId"),
                eq(ChatMessage.class), eq(String.class))).thenReturn(List.of("1_2"));
        when(mongoTemplate.findOne(any(Query.class), eq(ChatMessage.class))).thenReturn(latestMessage);
        when(userService.getUserSummary(2L)).thenReturn(otherUser, sender);

        var conversations = chatService.getConversations();

        assertEquals(1, conversations.size());
        assertEquals(2L, conversations.get(0).getUser().getId());
        assertEquals("Hello", conversations.get(0).getLatestMessage().getContent());
        assertEquals(createdAt, conversations.get(0).getLatestMessage().getCreatedAt());
        verify(mongoTemplate).findOne(any(Query.class), eq(ChatMessage.class));
    }
}
