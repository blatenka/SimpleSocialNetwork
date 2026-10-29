package com.aloute;

import com.aloute.model.entity.Friendship;
import com.aloute.model.entity.User;
import com.aloute.model.enums.FriendshipStatus;
import com.aloute.repository.mysql.FriendshipRepository;
import com.aloute.service.FriendshipService;
import com.aloute.service.NotificationService;
import com.aloute.service.UserService;
import com.aloute.service.impl.FriendshipServiceImpl;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FriendshipServiceTest {

    private final FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final FriendshipService service = new FriendshipServiceImpl(
            friendshipRepository, securityUtils, mock(NotificationService.class), mock(UserService.class));

    @Test
    void acceptingIncomingRequestPersistsAcceptedStatus() {
        Friendship request = Friendship.builder()
                .id(9L)
                .requesterId(1L)
                .addresseeId(2L)
                .status(FriendshipStatus.PENDING)
                .build();
        when(friendshipRepository.findById(9L)).thenReturn(Optional.of(request));
        when(securityUtils.getCurrentUserId()).thenReturn(2L);

        service.acceptRequest(9L);

        ArgumentCaptor<Friendship> saved = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(saved.capture());
        assertEquals(FriendshipStatus.ACCEPTED, saved.getValue().getStatus());
    }

    @Test
    void rejectingIncomingRequestPersistsDeniedStatus() {
        Friendship request = Friendship.builder()
                .id(10L)
                .requesterId(1L)
                .addresseeId(2L)
                .status(FriendshipStatus.PENDING)
                .build();
        when(friendshipRepository.findById(10L)).thenReturn(Optional.of(request));
        when(securityUtils.getCurrentUserId()).thenReturn(2L);

        service.declineRequest(10L);

        ArgumentCaptor<Friendship> saved = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(saved.capture());
        assertEquals(FriendshipStatus.DENIED, saved.getValue().getStatus());
    }

    @Test
    void mutualFriendsAreDistinctAndDerivedFromBothAcceptedFriendLists() {
        UserService userService = org.mockito.Mockito.mock(UserService.class);
        FriendshipServiceImpl mutualService = new FriendshipServiceImpl(
                friendshipRepository, securityUtils, org.mockito.Mockito.mock(NotificationService.class), userService);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(userService.getUserById(9L)).thenReturn(User.builder().id(9L).build());
        when(friendshipRepository.findFriendIdsByUserId(1L)).thenReturn(List.of(2L, 3L, 2L, 5L));
        when(friendshipRepository.findFriendIdsByUserId(9L)).thenReturn(List.of(3L, 2L, 4L));
        when(userService.getUserSummaries(List.of(2L, 3L))).thenReturn(List.of(
                UserSummaryResponseDto.builder().id(2L).fullname("Alex").build(),
                UserSummaryResponseDto.builder().id(3L).fullname("Sam").build()));

        List<UserSummaryResponseDto> mutualFriends = mutualService.getMutualFriends(9L);

        assertEquals(List.of(2L, 3L), mutualFriends.stream().map(UserSummaryResponseDto::getId).toList());
    }
}
