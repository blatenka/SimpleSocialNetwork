package com.aloute.dto.notification.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.TargetType;
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
public class NotificationResponseDto {
    private String id;
    private NotificationType type;
    private UserSummaryResponseDto actor;
    private String targetId;
    private TargetType targetType;
    private String content;
    private String linkUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
