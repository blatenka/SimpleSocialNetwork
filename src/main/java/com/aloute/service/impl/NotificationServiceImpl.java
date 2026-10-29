package com.aloute.service.impl;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.notification.response.NotificationResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.mapper.NotificationMapper;
import com.aloute.model.document.Notification;
import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.TargetType;
import com.aloute.repository.mongo.NotificationRepository;
import com.aloute.service.NotificationService;
import com.aloute.service.UserService;
import com.aloute.util.SecurityUtils;
import com.aloute.websocket.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Override
    public Notification createNotification(Long receiverId, Long actorId, NotificationType type, String targetId,
                                           TargetType targetType, String content, String linkUrl) {
        Notification n = Notification.builder()
                .receiverId(receiverId)
                .actorId(actorId)
                .type(type)
                .targetId(targetId)
                .targetType(targetType)
                .content(content)
                .linkUrl(linkUrl)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(n);
        NotificationResponseDto response = NotificationMapper.toResponse(
                saved, actorId == null ? null : userService.getUserSummary(actorId));
        realtimeEventPublisher.publishNotification(
                receiverId, response, notificationRepository.countByReceiverIdAndIsReadFalse(receiverId));
        return saved;
    }

    @Override
    public PageResponseDto<NotificationResponseDto> getNotifications(Long userId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Notification> notifications = notificationRepository
                .findByReceiverIdOrderByCreatedAtDescIdDesc(userId, pr);

        List<Long> actorIds = notifications.getContent().stream()
                .map(Notification::getActorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserSummaryResponseDto> actors = userService.getUserSummaries(actorIds).stream()
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, Function.identity()));
        return PageResponseDto.from(
                notifications.map(n -> NotificationMapper.toResponse(n, actors.get(n.getActorId()))));
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(String notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại"));
        if (!java.util.Objects.equals(n.getReceiverId(), securityUtils.getCurrentUserId())) {
            throw new ResourceNotFoundException("Thông báo không tồn tại");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
        realtimeEventPublisher.publishNotificationCount(
                n.getReceiverId(), notificationRepository.countByReceiverIdAndIsReadFalse(n.getReceiverId()));
    }

    @Override
    public void markAllAsRead(Long userId) {
        Query q = new Query(Criteria.where("receiverId").is(userId).and("isRead").is(false));
        Update u = new Update().set("isRead", true);
        mongoTemplate.updateMulti(q, u, Notification.class);
        realtimeEventPublisher.publishNotificationCount(userId, notificationRepository.countByReceiverIdAndIsReadFalse(userId));
    }
}