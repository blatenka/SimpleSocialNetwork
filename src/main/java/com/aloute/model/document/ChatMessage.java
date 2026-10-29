package com.aloute.model.document;

import com.aloute.model.enums.MessageType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "conv_time_idx", def = "{'conversationId': 1, 'createdAt': -1}")
public class ChatMessage {
    @Id
    private String id;

    @Indexed
    private String conversationId;

    private Long senderId;
    private Long recipientId;
    private String content;
    private String mediaUrl;
    
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
    
    @Builder.Default
    private Boolean isRead = false;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private Boolean isDeleted = false;
}