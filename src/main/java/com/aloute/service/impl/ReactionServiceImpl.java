package com.aloute.service.impl;

import com.aloute.model.entity.UserInfo;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.dto.reaction.response.ReactionUserResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.exception.BadRequestException;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.model.document.Comment;
import com.aloute.model.document.Post;
import com.aloute.model.document.Reaction;
import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.ReactionType;
import com.aloute.model.enums.TargetType;
import com.aloute.repository.mongo.CommentRepository;
import com.aloute.repository.mongo.PostRepository;
import com.aloute.repository.mongo.ReactionRepository;
import com.aloute.service.UserService;
import com.aloute.service.NotificationService;
import com.aloute.service.ReactionService;
import com.aloute.service.FriendshipService;
import com.aloute.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final MongoTemplate mongoTemplate;
    private final SecurityUtils securityUtils;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final FriendshipService friendshipService;

    @Override
    public ReactionSummaryResponseDto toggleReaction(String targetId, TargetType targetType, ReactionType reactionType) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (reactionType == null) throw new BadRequestException("Reaction type is required");
        validateTarget(targetId, targetType);

        Optional<Reaction> existingReactionOpt = reactionRepository.findByUserIdAndTargetIdAndTargetType(
                currentUserId, targetId, targetType
        );

        if (existingReactionOpt.isPresent()) {
            Reaction existingReaction = existingReactionOpt.get();

            if (existingReaction.getReactionType() == reactionType) {
                reactionRepository.delete(existingReaction);
                updateReactionCount(targetId, targetType, -1);
            } else {
                existingReaction.setReactionType(reactionType);
                reactionRepository.save(existingReaction);
            }
        } else {
            Reaction newReaction = Reaction.builder()
                    .userId(currentUserId)
                    .targetId(targetId)
                    .targetType(targetType)
                    .reactionType(reactionType)
                    .build();

            reactionRepository.save(newReaction);
            updateReactionCount(targetId, targetType, 1);

            // Notification
            try {
                String actorName = "Someone";
                try { UserInfo ui = userService.getUserProfile(currentUserId); if (ui.getFullname() != null) actorName = ui.getFullname(); } catch (Exception ignored) {}
                if (targetType == TargetType.POST) {
                    Post post = postRepository.findById(targetId).orElse(null);
                    if (post != null && !post.getUserId().equals(currentUserId)) {
                        notificationService.createNotification(post.getUserId(), currentUserId,
                                NotificationType.REACTION, targetId, TargetType.POST,
                                actorName + " đã thích bài viết của bạn", "/posts/" + targetId);
                    }
                } else if (targetType == TargetType.COMMENT) {
                    Comment comment = commentRepository.findById(targetId).orElse(null);
                    if (comment != null && !comment.getUserId().equals(currentUserId)) {
                        notificationService.createNotification(comment.getUserId(), currentUserId,
                                NotificationType.REACTION, targetId, TargetType.COMMENT,
                                actorName + " đã thích bình luận của bạn", "/posts/" + comment.getPostId());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return summarize(reactionRepository.findByTargetIdAndTargetType(targetId, targetType), currentUserId);
    }

    @Override
    public Map<String, ReactionSummaryResponseDto> getSummaries(Collection<String> targetIds, TargetType targetType) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Map<String, List<Reaction>> reactions = targetIds.isEmpty()
                ? Map.of()
                : reactionRepository.findByTargetTypeAndTargetIdIn(targetType, targetIds).stream()
                .collect(Collectors.groupingBy(Reaction::getTargetId));
        Map<String, ReactionSummaryResponseDto> summaries = new LinkedHashMap<>();
        for (String targetId : targetIds) {
            summaries.put(targetId, summarize(reactions.getOrDefault(targetId, List.of()), currentUserId));
        }
        return summaries;
    }

    @Override
    public List<ReactionUserResponseDto> getReactors(String targetId, TargetType targetType) {
        validateTarget(targetId, targetType);
        List<Reaction> reactions = reactionRepository.findByTargetIdAndTargetType(targetId, targetType);
        Map<Long, UserSummaryResponseDto> authors = userService.getUserSummaries(
                        reactions.stream().map(Reaction::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, user -> user));
        return reactions.stream().map(reaction -> ReactionUserResponseDto.builder()
                .author(authors.get(reaction.getUserId()))
                .reactionType(reaction.getReactionType())
                .build()).toList();
    }

    private ReactionSummaryResponseDto summarize(List<Reaction> reactions, Long currentUserId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReactionType type : ReactionType.values()) counts.put(type.name(), 0L);
        ReactionType currentUserReaction = null;
        for (Reaction reaction : reactions) {
            counts.compute(reaction.getReactionType().name(), (key, count) -> count + 1);
            if (reaction.getUserId().equals(currentUserId)) currentUserReaction = reaction.getReactionType();
        }
        return ReactionSummaryResponseDto.builder()
                .counts(counts)
                .currentUserReaction(currentUserReaction)
                .build();
    }

    private void validateTarget(String targetId, TargetType targetType) {
        if (targetType == TargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
            if (Boolean.TRUE.equals(post.getIsDeleted())) {
                throw new ResourceNotFoundException("Post not found");
            }
            ensureCanView(post);
        } else if (targetType == TargetType.COMMENT) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
            Post post = postRepository.findById(comment.getPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
            ensureCanView(post);
        } else {
            throw new BadRequestException("Unsupported reaction target");
        }
    }

    private void ensureCanView(Post post) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (post.getUserId().equals(currentUserId) || post.getPrivacy() == null
                || post.getPrivacy() == com.aloute.model.enums.Privacy.PUBLIC) return;
        if (post.getPrivacy() == com.aloute.model.enums.Privacy.FRIENDS
                && friendshipService.areFriends(post.getUserId(), currentUserId)) return;
        throw new AccessDeniedException("You cannot interact with this post");
    }

    private void updateReactionCount(String targetId, TargetType targetType, int incrementValue) {
        Query query = new Query(Criteria.where("id").is(targetId));

        if (targetType == TargetType.POST) {
            Update update = new Update().inc("likeCount", incrementValue);
            mongoTemplate.updateFirst(query, update, Post.class);
        }
    }
}