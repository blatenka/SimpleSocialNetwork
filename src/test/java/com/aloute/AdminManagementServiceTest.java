package com.aloute;

import com.aloute.dto.admin.request.AdminCreateUserRequestDto;
import com.aloute.dto.admin.request.AdminUpdateUserRequestDto;
import com.aloute.model.document.Comment;
import com.aloute.model.document.Post;
import com.aloute.model.entity.User;
import com.aloute.model.entity.UserInfo;
import com.aloute.model.enums.Role;
import com.aloute.model.enums.TargetType;
import com.aloute.model.enums.UserStatus;
import com.aloute.repository.mongo.CommentRepository;
import com.aloute.repository.mongo.PostRepository;
import com.aloute.repository.mongo.ReactionRepository;
import com.aloute.repository.mysql.UserInfoRepository;
import com.aloute.repository.mysql.UserRepository;
import com.aloute.service.FriendshipService;
import com.aloute.service.ReactionService;
import com.aloute.service.UserService;
import com.aloute.service.impl.PostServiceImpl;
import com.aloute.service.impl.UserServiceImpl;
import com.aloute.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminManagementServiceTest {

    @Test
    void adminCreatedUsersAreNormalizedAndCannotReceiveAdminRole() {
        UserRepository users = mock(UserRepository.class);
        UserInfoRepository profiles = mock(UserInfoRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(profiles, users, mock(SecurityUtils.class), encoder);
        AdminCreateUserRequestDto request = new AdminCreateUserRequestDto();
        request.setEmail(" New.User@Example.test ");
        request.setPassword("temporary-pass");
        request.setFullname("New User");
        request.setNickname("newbie");
        when(users.existsByEmail("new.user@example.test")).thenReturn(false);
        when(encoder.encode("temporary-pass")).thenReturn("encoded");
        AtomicReference<User> savedUser = new AtomicReference<>();
        when(users.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            savedUser.set(saved);
            return saved;
        });

        var response = service.createAdminUser(request);

        assertEquals(42L, response.getId());
        assertEquals("new.user@example.test", response.getEmail());
        assertEquals(Role.USER.name(), response.getRole());
        assertEquals("New User", response.getFullname());
        assertEquals("encoded", savedUser.get().getPassword());
        verify(users).existsByEmail("new.user@example.test");
    }

    @Test
    void adminCanEditProfileAndSuspendWithoutChangingRole() {
        UserRepository users = mock(UserRepository.class);
        UserInfoRepository profiles = mock(UserInfoRepository.class);
        SecurityUtils security = mock(SecurityUtils.class);
        UserServiceImpl service = new UserServiceImpl(profiles, users, security, mock(PasswordEncoder.class));
        User user = User.builder().id(7L).email("person@example.test").role(Role.USER)
                .status(UserStatus.ACTIVE).build();
        UserInfo profile = UserInfo.builder().id(7L).user(user).fullname("Before").build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(profiles.findById(7L)).thenReturn(Optional.of(profile));
        when(security.getCurrentUserId()).thenReturn(99L);
        when(users.save(user)).thenReturn(user);
        when(profiles.save(profile)).thenReturn(profile);
        AdminUpdateUserRequestDto request = new AdminUpdateUserRequestDto();
        request.setFullname("After");
        request.setStatus(UserStatus.BANNED);

        var response = service.updateAdminUser(7L, request);

        assertEquals("After", response.getFullname());
        assertEquals(UserStatus.BANNED.name(), response.getStatus());
        assertEquals(Role.USER.name(), response.getRole());
        verify(users).save(user);
        verify(profiles).save(profile);
    }

    @Test
    void adminPostDeletionRemovesCommentsAndReactionsBeforeSoftDeletingPost() {
        PostRepository posts = mock(PostRepository.class);
        CommentRepository comments = mock(CommentRepository.class);
        ReactionRepository reactions = mock(ReactionRepository.class);
        PostServiceImpl service = new PostServiceImpl(posts, mock(SecurityUtils.class), mock(UserService.class),
                mock(ReactionService.class), mock(FriendshipService.class), mock(MongoTemplate.class),
                comments, reactions);
        Post post = Post.builder().id("post-1").userId(10L).isDeleted(false).build();
        Comment comment = Comment.builder().id("comment-1").postId("post-1").build();
        when(posts.findById("post-1")).thenReturn(Optional.of(post));
        when(comments.findIdsByPostId("post-1")).thenReturn(List.of(comment));
        when(posts.save(post)).thenReturn(post);

        service.adminDeletePost("post-1");

        assertTrue(post.getIsDeleted());
        verify(reactions).deleteByTargetIdAndTargetType("post-1", TargetType.POST);
        verify(reactions).deleteByTargetTypeAndTargetIdIn(TargetType.COMMENT, List.of("comment-1"));
        verify(comments).deleteByPostId("post-1");
        verify(posts).save(post);
    }
}
