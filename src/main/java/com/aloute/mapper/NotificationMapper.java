package com.aloute.mapper;

import com.aloute.dto.notification.response.NotificationResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.document.Notification;

public final class NotificationMapper {
    private NotificationMapper() {}

    public static NotificationResponseDto toResponse(Notification n, UserSummaryResponseDto actor) {
        if (n == null) return null;
        return NotificationResponseDto.builder()
                .id(n.getId())
                .type(n.getType())
                .actor(actor)
                .targetId(n.getTargetId())
                .targetType(n.getTargetType())
                .content(n.getContent())
                .linkUrl(n.getLinkUrl())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
