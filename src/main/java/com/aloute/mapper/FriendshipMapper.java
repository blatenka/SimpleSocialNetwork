package com.aloute.mapper;

import com.aloute.dto.friendship.response.FriendshipResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.entity.Friendship;

public final class FriendshipMapper {
    private FriendshipMapper() {}

    public static FriendshipResponseDto toResponse(Friendship f) {
        return toResponse(f, null, null);
    }

    public static FriendshipResponseDto toResponse(Friendship f, UserSummaryResponseDto requester,
                                                   UserSummaryResponseDto addressee) {
        if (f == null) return null;
        return FriendshipResponseDto.builder()
                .id(f.getId())
                .requesterId(f.getRequesterId())
                .addresseeId(f.getAddresseeId())
                .requester(requester)
                .addressee(addressee)
                .status(f.getStatus())
                .build();
    }
}
