package com.aloute.repository.mongo;

import com.aloute.model.document.Post;
import com.aloute.model.enums.Privacy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    
    Page<Post> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);
    
    Page<Post> findByPrivacyAndIsDeletedFalse(Privacy privacy, Pageable pageable);

    Page<Post> findByIsDeletedFalseAndPrivacyInAndUserIdIn(List<Privacy> privacies, List<Long> userIds, Pageable pageable);

    Page<Post> findByIsDeletedFalse(Pageable pageable);

    Page<Post> findByContentContainingIgnoreCaseAndIsDeletedFalse(String keyword, Pageable pageable);

    long countByUserId(Long userId);

    long countByIsDeletedFalse();

    long countByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime start, LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
