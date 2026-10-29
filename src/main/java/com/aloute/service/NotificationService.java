package com.aloute.service;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.notification.response.NotificationResponseDto;
import com.aloute.model.document.Notification;
import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.TargetType;

public interface NotificationService {
    Notification createNotification(Long receiverId, Long actorId, NotificationType type,
                                    String targetId, TargetType targetType,
                                    String content, String linkUrl);
    PageResponseDto<NotificationResponseDto> getNotifications(Long userId, int page, int size);
    long getUnreadCount(Long userId);
    void markAsRead(String notificationId);
    void markAllAsRead(Long userId);
}
