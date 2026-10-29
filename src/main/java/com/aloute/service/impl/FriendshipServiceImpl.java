package com.aloute.service.impl;

import com.aloute.exception.BadRequestException;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.dto.friendship.response.FriendshipResponseDto;
import com.aloute.dto.friendship.response.FriendshipStatusResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.mapper.FriendshipMapper;
import com.aloute.model.entity.Friendship;
import com.aloute.model.enums.FriendshipStatus;
import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.TargetType;
import com.aloute.model.entity.UserInfo;
import com.aloute.service.UserService;
import com.aloute.repository.mysql.FriendshipRepository;
import com.aloute.service.FriendshipService;
import com.aloute.service.NotificationService;
import com.aloute.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final UserService userService;

    @Override
    public FriendshipResponseDto sendRequest(Long addresseeId) {
        Long requesterId = securityUtils.getCurrentUserId();
        if (requesterId.equals(addresseeId)) {
            throw new BadRequestException("Không thể gửi lời mời cho chính mình");
        }
        Optional<Friendship> existing = friendshipRepository.findFriendshipBetween(requesterId, addresseeId);
        if (existing.isPresent()) {
            throw new BadRequestException("Đã tồn tại mối quan hệ giữa hai người");
        }
        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .status(FriendshipStatus.PENDING)
                .build();
        Friendship saved = friendshipRepository.save(friendship);

        try {
            String actorName = "Someone";
            try { UserInfo ui = userService.getUserProfile(requesterId); if (ui.getFullname() != null) actorName = ui.getFullname(); } catch (Exception ignored) {}
            notificationService.createNotification(addresseeId, requesterId,
                    NotificationType.FRIEND_REQUEST, saved.getId().toString(), TargetType.FRIENDSHIP,
                    actorName + " đã gửi lời mời kết bạn", "/friends/requests");
        } catch (Exception ignored) {
        }
        return toResponse(saved);
    }

    @Override
    public void acceptRequest(Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời"));
        Long current = securityUtils.getCurrentUserId();
        if (!f.getAddresseeId().equals(current)) {
            throw new BadRequestException("Không có quyền chấp nhận lời mời này");
        }
        f.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(f);

        try {
            String actorName = "Someone";
            try { UserInfo ui = userService.getUserProfile(current); if (ui.getFullname() != null) actorName = ui.getFullname(); } catch (Exception ignored) {}
            notificationService.createNotification(f.getRequesterId(), current,
                    NotificationType.FRIEND_ACCEPT, f.getId().toString(), TargetType.FRIENDSHIP,
                    actorName + " đã chấp nhận lời mời kết bạn", "/friends/list");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void declineRequest(Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời"));
        Long current = securityUtils.getCurrentUserId();
        if (!f.getAddresseeId().equals(current)) {
            throw new BadRequestException("Không có quyền từ chối lời mời này");
        }
        f.setStatus(FriendshipStatus.DENIED);
        friendshipRepository.save(f);
    }

    @Override
    public void cancelRequest(Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời"));
        Long current = securityUtils.getCurrentUserId();
        if (!f.getRequesterId().equals(current)) {
            throw new BadRequestException("Không có quyền huỷ lời mời này");
        }
        friendshipRepository.delete(f);
    }

    @Override
    public void unfriend(Long otherUserId) {
        Long current = securityUtils.getCurrentUserId();
        Optional<Friendship> f = friendshipRepository.findFriendshipBetween(current, otherUserId);
        if (f.isEmpty()) {
            throw new ResourceNotFoundException("Không tồn tại mối quan hệ");
        }
        friendshipRepository.delete(f.get());
    }

    @Override
    public List<FriendshipResponseDto> getPendingRequests(Long userId) {
        List<Friendship> incoming = friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING);
        List<Friendship> outgoing = friendshipRepository.findByRequesterIdAndStatus(userId, FriendshipStatus.PENDING);
        return toResponses(java.util.stream.Stream.concat(incoming.stream(), outgoing.stream()).toList());
    }

    @Override
    public List<Long> getFriendIds(Long userId) {
        return friendshipRepository.findFriendIdsByUserId(userId);
    }

    @Override
    public List<UserSummaryResponseDto> getFriends(Long userId) {
        return userService.getUserSummaries(getFriendIds(userId));
    }

    @Override
    public List<UserSummaryResponseDto> getMutualFriends(Long otherUserId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        userService.getUserById(otherUserId);
        if (currentUserId.equals(otherUserId)) return List.of();

        List<Long> currentFriendIds = getFriendIds(currentUserId);
        List<Long> otherFriendIds = getFriendIds(otherUserId);
        java.util.Set<Long> otherFriendSet = new java.util.HashSet<>(otherFriendIds);
        List<Long> mutualIds = currentFriendIds.stream()
                .filter(otherFriendSet::contains)
                .distinct()
                .toList();
        return userService.getUserSummaries(mutualIds);
    }

    @Override
    public boolean areFriends(Long user1, Long user2) {
        return friendshipRepository.findFriendshipBetween(user1, user2)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    @Override
    public FriendshipStatus getStatusBetween(Long user1, Long user2) {
        return friendshipRepository.findFriendshipBetween(user1, user2)
                .map(Friendship::getStatus)
                .orElse(null);
    }

    @Override
    public FriendshipStatusResponseDto getStatusWithDirection(Long user1, Long user2) {
        Optional<Friendship> f = friendshipRepository.findFriendshipBetween(user1, user2);
        if (f.isEmpty()) {
            return FriendshipStatusResponseDto.builder().build();
        }
        Friendship friendship = f.get();
        String direction;
        if (friendship.getRequesterId().equals(user1)) {
            direction = "outgoing";
        } else {
            direction = "incoming";
        }
        return FriendshipStatusResponseDto.builder()
                .status(friendship.getStatus().name())
                .direction(direction)
                .build();
    }

    private FriendshipResponseDto toResponse(Friendship friendship) {
        return FriendshipMapper.toResponse(friendship,
                userService.getUserSummary(friendship.getRequesterId()),
                userService.getUserSummary(friendship.getAddresseeId()));
    }

    private List<FriendshipResponseDto> toResponses(List<Friendship> friendships) {
        List<Long> userIds = friendships.stream()
                .flatMap(friendship -> java.util.stream.Stream.of(
                        friendship.getRequesterId(), friendship.getAddresseeId()))
                .distinct()
                .toList();
        Map<Long, UserSummaryResponseDto> summaries = userService.getUserSummaries(userIds).stream()
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, Function.identity()));
        return friendships.stream()
                .map(friendship -> FriendshipMapper.toResponse(
                        friendship,
                        summaries.get(friendship.getRequesterId()),
                        summaries.get(friendship.getAddresseeId())))
                .toList();
    }
}