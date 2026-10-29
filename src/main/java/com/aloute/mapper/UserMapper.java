package com.aloute.mapper;

import com.aloute.dto.user.response.UserInfoResponseDto;
import com.aloute.dto.user.response.UserProfileResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.entity.User;
import com.aloute.model.entity.UserInfo;

public final class UserMapper {
    private UserMapper() {}

    public static UserSummaryResponseDto toSummary(UserInfo info) {
        if (info == null) return null;
        return UserSummaryResponseDto.builder()
                .id(info.getId())
                .fullname(info.getFullname())
                .nickname(info.getNickname())
                .avatarUrl(info.getAvatarUrl())
                .build();
    }

    public static UserInfoResponseDto toInfoResponse(UserInfo info) {
        if (info == null) return null;
        return UserInfoResponseDto.builder()
                .userId(info.getId())
                .fullname(info.getFullname())
                .nickname(info.getNickname())
                .avatarUrl(info.getAvatarUrl())
                .coverUrl(info.getCoverUrl())
                .description(info.getDescription())
                .dob(info.getDob())
                .hometown(info.getHometown())
                .build();
    }

    public static UserProfileResponseDto toProfileResponse(User user, UserInfo info) {
        if (user == null || info == null) return null;
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .fullname(info.getFullname())
                .nickname(info.getNickname())
                .avatarUrl(info.getAvatarUrl())
                .coverUrl(info.getCoverUrl())
                .description(info.getDescription())
                .dob(info.getDob())
                .hometown(info.getHometown())
                .build();
    }
}
