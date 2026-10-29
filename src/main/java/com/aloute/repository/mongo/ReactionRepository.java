package com.aloute.repository.mongo;

import com.aloute.model.document.Reaction;
import com.aloute.model.enums.ReactionType;
import com.aloute.model.enums.TargetType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReactionRepository extends MongoRepository<Reaction, String> {
    
    Optional<Reaction> findByUserIdAndTargetIdAndTargetType(Long userId, String targetId, TargetType targetType);

    List<Reaction> findByTargetTypeAndTargetIdIn(TargetType targetType, Collection<String> targetIds);

    List<Reaction> findByTargetIdAndTargetType(String targetId, TargetType targetType);

    long countByTargetIdAndTargetTypeAndReactionType(
            String targetId, TargetType targetType, ReactionType reactionType);
    
    void deleteByUserIdAndTargetIdAndTargetType(Long userId, String targetId, TargetType targetType);
    
    void deleteByTargetIdAndTargetType(String targetId, TargetType targetType);

    void deleteByTargetTypeAndTargetIdIn(TargetType targetType, Collection<String> targetIds);
}