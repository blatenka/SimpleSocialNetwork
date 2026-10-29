package com.aloute.model.document;

import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.TargetType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "receiver_time_idx", def = "{'receiverId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "receiver_unread_idx", def = "{'receiverId': 1, 'isRead': 1}")
})
public class Notification {
    @Id
    private String id;
    private Long receiverId;
    private Long actorId;
    private NotificationType type;
    private String targetId;
    private TargetType targetType;
    private String content;
    private String linkUrl;
    
    @Builder.Default
    private Boolean isRead = false;
    private LocalDateTime createdAt;
}