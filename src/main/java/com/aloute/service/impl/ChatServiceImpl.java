package com.aloute.service.impl;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.chat.response.MessageResponseDto;
import com.aloute.dto.chat.response.ConversationResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.mapper.ChatMapper;
import com.aloute.model.document.ChatMessage;
import com.aloute.exception.BadRequestException;
import com.aloute.model.enums.MessageType;
import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.TargetType;
import com.aloute.repository.mongo.ChatMessageRepository;
import com.aloute.service.NotificationService;
import com.aloute.service.UserService;
import java.time.LocalDateTime;
import com.aloute.service.ChatService;
import com.aloute.util.SecurityUtils;
import com.aloute.websocket.RealtimeEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatMessageRepository chatMessageRepository;
    private final MongoTemplate mongoTemplate;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final UserService userService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    private String conversationId(Long u1, Long u2) {
        long a = Math.min(u1, u2);
        long b = Math.max(u1, u2);
        return a + "_" + b;
    }

    private UserSummaryResponseDto getSender(Long userId) {
        if (userId == null) return null;
        return userService.getUserSummary(userId);
    }

    @Override
    public List<ConversationResponseDto> getConversations() {
        Long currentUserId = securityUtils.getCurrentUserId();
        Query conversationsQuery = Query.query(new Criteria().andOperator(
                new Criteria().orOperator(
                        Criteria.where("senderId").is(currentUserId),
                        Criteria.where("recipientId").is(currentUserId)),
                Criteria.where("isDeleted").ne(true)));
        List<String> conversationIds = mongoTemplate.findDistinct(
                conversationsQuery, "conversationId", ChatMessage.class, String.class);

        List<ConversationResponseDto> conversations = new ArrayList<>();
        for (String id : conversationIds) {
            Query latestMessageQuery = Query.query(Criteria.where("conversationId").is(id)
                    .and("isDeleted").ne(true))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .limit(1);
            ChatMessage latestMessage = mongoTemplate.findOne(latestMessageQuery, ChatMessage.class);
            if (latestMessage == null) {
                continue;
            }

            Long otherUserId = latestMessage.getSenderId().equals(currentUserId)
                    ? latestMessage.getRecipientId()
                    : latestMessage.getSenderId();
            conversations.add(ConversationResponseDto.builder()
                    .user(userService.getUserSummary(otherUserId))
                    .latestMessage(ChatMapper.toResponse(latestMessage,
                            getSender(latestMessage.getSenderId())))
                            .unreadCount(chatMessageRepository.countBySenderIdAndRecipientIdAndIsReadFalse(
                                    otherUserId, currentUserId))
                            .build());
        }

        conversations.sort(Comparator.comparing(
                (ConversationResponseDto conversation) -> conversation.getLatestMessage().getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return conversations;
    }

    @Override
    public MessageResponseDto sendMessage(Long recipientId, String content, String mediaUrl) {
        if ((content == null || content.isBlank()) && (mediaUrl == null || mediaUrl.isBlank())) {
            throw new BadRequestException("Message content or media is required");
        }
        Long senderId = securityUtils.getCurrentUserId();
        ChatMessage msg = ChatMessage.builder()
                .conversationId(conversationId(senderId, recipientId))
                .senderId(senderId)
                .recipientId(recipientId)
                .content(content)
                .mediaUrl(mediaUrl)
                .messageType(mediaUrl != null ? MessageType.IMAGE : MessageType.TEXT)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        ChatMessage saved = chatMessageRepository.save(msg);

        MessageResponseDto response = ChatMapper.toResponse(saved, getSender(senderId));
        try {
            String actorName = response.getSender() != null && response.getSender().getFullname() != null
                    ? response.getSender().getFullname()
                    : "Someone";
            notificationService.createNotification(recipientId, senderId,
                    NotificationType.NEW_MESSAGE, saved.getId(), TargetType.USER,
                    actorName + " đã gửi tin nhắn cho bạn", "/chat?userId=" + senderId);
        } catch (RuntimeException exception) {
            logger.error("Message {} was saved but its notification could not be created", saved.getId(), exception);
        }

        realtimeEventPublisher.publishChatMessage(recipientId, response);
        realtimeEventPublisher.publishChatMessage(senderId, response);
        realtimeEventPublisher.publishUnreadMessageCount(
                recipientId, senderId,
                chatMessageRepository.countBySenderIdAndRecipientIdAndIsReadFalse(senderId, recipientId));
        return response;
    }

    @Override
    public PageResponseDto<MessageResponseDto> getHistory(Long otherUserId, int page, int size) {
        Long current = securityUtils.getCurrentUserId();
        String conv = conversationId(current, otherUserId);
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Một cuộc hội thoại chỉ có 2 người, nên cache để mỗi người chỉ tra cứu một lần
        Map<Long, UserSummaryResponseDto> senderCache = new HashMap<>();

        Page<MessageResponseDto> messages = chatMessageRepository.findByConversationIdAndIsDeletedFalse(conv, pr)
                .map(m -> ChatMapper.toResponse(m,
                        senderCache.computeIfAbsent(m.getSenderId(), this::getSender)));
        return PageResponseDto.from(messages);
    }

    @Override
    public void markAsRead(Long otherUserId) {
        Long current = securityUtils.getCurrentUserId();
        Query q = new Query(Criteria.where("conversationId").is(conversationId(current, otherUserId))
                .and("recipientId").is(current).and("isRead").is(false));
        Update u = new Update().set("isRead", true).set("readAt", LocalDateTime.now());
        mongoTemplate.updateMulti(q, u, ChatMessage.class);
        realtimeEventPublisher.publishUnreadMessageCount(
                current, otherUserId,
                chatMessageRepository.countBySenderIdAndRecipientIdAndIsReadFalse(otherUserId, current));
        realtimeEventPublisher.publishUnreadMessageCount(
                otherUserId, current,
                chatMessageRepository.countBySenderIdAndRecipientIdAndIsReadFalse(current, otherUserId));
    }

    @Override
    public long getUnreadCount(Long otherUserId) {
        Long current = securityUtils.getCurrentUserId();
        return chatMessageRepository.countBySenderIdAndRecipientIdAndIsReadFalse(otherUserId, current);
    }
}