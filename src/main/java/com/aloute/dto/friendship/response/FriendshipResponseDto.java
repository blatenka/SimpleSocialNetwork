package com.aloute.dto.friendship.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.enums.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendshipResponseDto {
    private Long id;
    private Long requesterId;
    private Long addresseeId;
    private UserSummaryResponseDto requester;
    private UserSummaryResponseDto addressee;
    private FriendshipStatus status;
}
