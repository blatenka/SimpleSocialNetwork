package com.aloute;

import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.document.Comment;
import com.aloute.model.document.Post;
import com.aloute.model.enums.Privacy;
import com.aloute.model.enums.TargetType;
import com.aloute.repository.mongo.CommentRepository;
import com.aloute.repository.mongo.PostRepository;
import com.aloute.service.FriendshipService;
import com.aloute.service.NotificationService;
import com.aloute.service.ReactionService;
import com.aloute.service.UserService;
import com.aloute.service.impl.CommentServiceImpl;
import com.aloute.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private final CommentRepository comments = mock(CommentRepository.class);
    private final PostRepository posts = mock(PostRepository.class);
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final UserService userService = mock(UserService.class);
    private final ReactionService reactionService = mock(ReactionService.class);
    private final FriendshipService friendshipService = mock(FriendshipService.class);
    private final CommentServiceImpl service = new CommentServiceImpl(
            comments, posts, mongoTemplate, securityUtils, mock(NotificationService.class),
            userService, reactionService, friendshipService);

    @Test
    void replyStoresParentPostAndAuthenticatedAuthor() {
        Comment parent = Comment.builder().id("parent-1").postId("post-1").userId(7L).build();
        Post post = Post.builder().id("post-1").userId(3L).privacy(Privacy.PUBLIC).build();
        UserSummaryResponseDto author = UserSummaryResponseDto.builder().id(7L).fullname("Current User").build();
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        when(comments.findById("parent-1")).thenReturn(Optional.of(parent));
        when(posts.findById("post-1")).thenReturn(Optional.of(post));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId("reply-1");
            return saved;
        });
        when(userService.getUserSummary(7L)).thenReturn(author);
        when(reactionService.getSummaries(List.of("reply-1"), TargetType.COMMENT))
                .thenReturn(Map.of("reply-1", ReactionSummaryResponseDto.builder().counts(Map.of()).build()));

        var response = service.replyComment("parent-1", "  A reply  ");

        assertEquals("reply-1", response.getId());
        assertEquals("post-1", response.getPostId());
        assertEquals("parent-1", response.getParentId());
        assertEquals("A reply", response.getContent());
        assertEquals(7L, response.getAuthor().getId());
        verify(comments).save(any(Comment.class));
    }

    @Test
    void privatePostDoesNotAllowAnotherUserToReply() {
        Comment parent = Comment.builder().id("parent-2").postId("post-2").userId(9L).build();
        Post post = Post.builder().id("post-2").userId(9L).privacy(Privacy.PRIVATE).build();
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        when(comments.findById("parent-2")).thenReturn(Optional.of(parent));
        when(posts.findById("post-2")).thenReturn(Optional.of(post));

        assertThrows(AccessDeniedException.class, () -> service.replyComment("parent-2", "No access"));
    }
}
