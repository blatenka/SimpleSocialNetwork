package com.aloute.repository.mongo;

import com.aloute.model.document.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    Page<Notification> findByReceiverIdOrderByCreatedAtDescIdDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    void deleteByReceiverId(Long receiverId);
}