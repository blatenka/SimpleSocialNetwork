package com.aloute;

import com.aloute.exception.BadRequestException;
import com.aloute.model.document.Post;
import com.aloute.model.document.Reaction;
import com.aloute.model.enums.Privacy;
import com.aloute.model.enums.ReactionType;
import com.aloute.model.enums.TargetType;
import com.aloute.repository.mongo.CommentRepository;
import com.aloute.repository.mongo.PostRepository;
import com.aloute.repository.mongo.ReactionRepository;
import com.aloute.service.FriendshipService;
import com.aloute.service.NotificationService;
import com.aloute.service.UserService;
import com.aloute.service.impl.ReactionServiceImpl;
import com.aloute.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactionServiceTest {

    private final ReactionRepository reactionRepository = mock(ReactionRepository.class);
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final FriendshipService friendshipService = mock(FriendshipService.class);
    private final ReactionServiceImpl service = new ReactionServiceImpl(
            reactionRepository, mongoTemplate, securityUtils, postRepository, commentRepository,
            mock(NotificationService.class), mock(UserService.class), friendshipService);

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
    }

    @Test
    void changingReactionUpdatesTheExistingReactionAndReturnsCountsByType() {
        Post post = Post.builder().id("post-1").userId(7L).privacy(Privacy.PUBLIC).build();
        Reaction existing = Reaction.builder().id("reaction-1").userId(7L)
                .targetId("post-1").targetType(TargetType.POST).reactionType(ReactionType.LIKE).build();
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(reactionRepository.findByUserIdAndTargetIdAndTargetType(7L, "post-1", TargetType.POST))
                .thenReturn(Optional.of(existing));
        when(reactionRepository.findByTargetIdAndTargetType("post-1", TargetType.POST))
                .thenReturn(List.of(existing));

        var result = service.toggleReaction("post-1", TargetType.POST, ReactionType.LOVE);

        verify(reactionRepository).save(existing);
        assertEquals(ReactionType.LOVE, existing.getReactionType());
        assertEquals(0L, result.getCounts().get("LIKE"));
        assertEquals(1L, result.getCounts().get("LOVE"));
        assertEquals(ReactionType.LOVE, result.getCurrentUserReaction());
    }

    @Test
    void privatePostReactionsAreDeniedForNonOwners() {
        Post post = Post.builder().id("post-2").userId(9L).privacy(Privacy.PRIVATE).build();
        when(postRepository.findById("post-2")).thenReturn(Optional.of(post));

        assertThrows(AccessDeniedException.class,
                () -> service.toggleReaction("post-2", TargetType.POST, ReactionType.LIKE));
    }

    @Test
    void unsupportedTargetsAreRejected() {
        assertThrows(BadRequestException.class,
                () -> service.toggleReaction("target", TargetType.USER, ReactionType.LIKE));
    }
}
