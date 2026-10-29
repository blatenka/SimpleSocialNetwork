package com.aloute.repository.mongo;

import com.aloute.model.document.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    Page<ChatMessage> findByConversationIdAndIsDeletedFalse(String conversationId, Pageable pageable);
    
    long countBySenderIdAndRecipientIdAndIsReadFalse(Long senderId, Long recipientId);
}