package com.aloute.controller.api;

import com.aloute.dto.common.ApiMessageResponseDto;
import com.aloute.dto.friendship.response.FriendshipResponseDto;
import com.aloute.dto.friendship.response.FriendshipStatusResponseDto;
import com.aloute.dto.friendship.response.MutualFriendsResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.service.FriendshipService;
import com.aloute.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipApiController {

    private final FriendshipService friendshipService;
    private final UserService userService;

    @PostMapping("/request/{addresseeId}")
    public ResponseEntity<FriendshipResponseDto> sendRequest(@PathVariable Long addresseeId) {
        return ResponseEntity.ok(friendshipService.sendRequest(addresseeId));
    }

    @PostMapping("/accept/{id}")
    public ResponseEntity<ApiMessageResponseDto> accept(@PathVariable Long id) {
        friendshipService.acceptRequest(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Accepted").build());
    }

    @PostMapping("/decline/{id}")
    public ResponseEntity<ApiMessageResponseDto> decline(@PathVariable Long id) {
        friendshipService.declineRequest(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Declined").build());
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<ApiMessageResponseDto> cancel(@PathVariable Long id) {
        friendshipService.cancelRequest(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Cancelled").build());
    }

    @DeleteMapping("/unfriend/{userId}")
    public ResponseEntity<ApiMessageResponseDto> unfriend(@PathVariable Long userId) {
        friendshipService.unfriend(userId);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Unfriended").build());
    }

    @GetMapping("/list")
    public ResponseEntity<List<UserSummaryResponseDto>> friends() {
        Long current = userService.getCurrentUserDto().getId();
        return ResponseEntity.ok(friendshipService.getFriends(current));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipResponseDto>> pendingRequests() {
        Long current = userService.getCurrentUserDto().getId();
        return ResponseEntity.ok(friendshipService.getPendingRequests(current));
    }

    @GetMapping("/status/{otherId}")
    public ResponseEntity<FriendshipStatusResponseDto> status(@PathVariable Long otherId) {
        Long current = userService.getCurrentUserDto().getId();
        return ResponseEntity.ok(friendshipService.getStatusWithDirection(current, otherId));
    }

    @GetMapping("/mutual/{otherUserId}")
    public ResponseEntity<MutualFriendsResponseDto> mutualFriends(@PathVariable Long otherUserId) {
        List<UserSummaryResponseDto> friends = friendshipService.getMutualFriends(otherUserId);
        return ResponseEntity.ok(MutualFriendsResponseDto.builder()
                .count(friends.size())
                .friends(friends)
                .build());
    }
}
