package com.aloute.service;

import com.aloute.model.entity.Friendship;
import com.aloute.model.enums.FriendshipStatus;
import com.aloute.dto.friendship.response.FriendshipResponseDto;
import com.aloute.dto.friendship.response.FriendshipStatusResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;

import java.util.List;

public interface FriendshipService {
    FriendshipResponseDto sendRequest(Long addresseeId);
    void acceptRequest(Long friendshipId);
    void declineRequest(Long friendshipId);
    void cancelRequest(Long friendshipId);
    void unfriend(Long otherUserId);
    List<FriendshipResponseDto> getPendingRequests(Long userId);
    List<Long> getFriendIds(Long userId);
    List<UserSummaryResponseDto> getFriends(Long userId);
    List<UserSummaryResponseDto> getMutualFriends(Long otherUserId);
    boolean areFriends(Long user1, Long user2);
    FriendshipStatus getStatusBetween(Long user1, Long user2);
    FriendshipStatusResponseDto getStatusWithDirection(Long user1, Long user2);
}