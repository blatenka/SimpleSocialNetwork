package com.aloute;

import com.aloute.dto.user.request.UpdateUserInfoRequestDto;
import com.aloute.model.entity.UserInfo;
import com.aloute.repository.mysql.UserInfoRepository;
import com.aloute.repository.mysql.UserRepository;
import com.aloute.service.impl.UserServiceImpl;
import com.aloute.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserProfileImageTest {

    private final UserInfoRepository userInfoRepository = mock(UserInfoRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final UserServiceImpl userService = new UserServiceImpl(
            userInfoRepository, mock(UserRepository.class), securityUtils, mock(PasswordEncoder.class));

    @Test
    void persistsAvatarAndCoverUrlsForCurrentUser() {
        UserInfo profile = UserInfo.builder().id(4L).build();
        when(securityUtils.getCurrentUserId()).thenReturn(4L);
        when(userInfoRepository.findById(4L)).thenReturn(Optional.of(profile));
        when(userInfoRepository.save(profile)).thenReturn(profile);
        UpdateUserInfoRequestDto request = new UpdateUserInfoRequestDto();
        request.setAvatarUrl("https://images.example/avatar.png");
        request.setCoverUrl("https://images.example/cover.png");

        var response = userService.updateCurrentProfile(4L, request);

        assertEquals(request.getAvatarUrl(), response.getAvatarUrl());
        assertEquals(request.getCoverUrl(), response.getCoverUrl());
        verify(userInfoRepository).save(profile);
    }

    @Test
    void rejectsProfileImageUpdatesForAnotherUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(4L);

        assertThrows(AccessDeniedException.class,
                () -> userService.updateCurrentProfile(5L, new UpdateUserInfoRequestDto()));
        verifyNoInteractions(userInfoRepository);
    }
}
