package com.aloute.repository.mongo;

import com.aloute.model.document.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    Page<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(String postId, Pageable pageable);

    Page<Comment> findByPostIdOrderByCreatedAtDesc(String postId, Pageable pageable);
    
    Page<Comment> findByParentIdOrderByCreatedAtAsc(String parentId, Pageable pageable);

    long countByParentId(String parentId);

    long countByParentIdIsNotNull();

    @Query(value = "{ 'postId': ?0 }", fields = "{ '_id': 1 }")
    List<Comment> findIdsByPostId(String postId);

    @Query(value = "{ 'parentId': { $in: ?0 } }", fields = "{ 'parentId': 1 }")
    List<Comment> findReplyParentReferences(List<String> parentIds);
    
    void deleteByPostId(String postId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}