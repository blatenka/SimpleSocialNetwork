package com.aloute.service;

import com.aloute.dto.auth.response.CurrentUserResponseDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.admin.response.AdminUserResponseDto;
import com.aloute.dto.admin.request.AdminCreateUserRequestDto;
import com.aloute.dto.admin.request.AdminUpdateUserRequestDto;
import com.aloute.dto.user.response.UserInfoResponseDto;
import com.aloute.dto.user.response.UserProfileResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.dto.user.request.UpdateUserInfoRequestDto;
import com.aloute.model.entity.User;
import com.aloute.model.entity.UserInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    UserInfo getUserProfile(Long userId);
    User getUserById(Long userId);
    User getUserByEmail(String email);
    List<UserInfo> searchUsers(String keyword);
    void lockUser(Long userId);
    void unlockUser(Long userId);
    User getCurrentUser();

    // DTO helpers
    CurrentUserResponseDto getCurrentUserDto();
    UserProfileResponseDto getUserProfileDto(Long userId);
    UserInfoResponseDto updateCurrentProfile(Long requestedUserId, UpdateUserInfoRequestDto request);
    UserSummaryResponseDto getUserSummary(Long userId);
    List<UserSummaryResponseDto> getUserSummaries(List<Long> userIds);
    List<UserSummaryResponseDto> searchUserSummaries(String keyword);
    List<UserSummaryResponseDto> searchUserSummariesByFullname(String fullname);
    PageResponseDto<AdminUserResponseDto> getAdminUserDtos(int page, int size);
    PageResponseDto<AdminUserResponseDto> getAdminUserDtos(int page, int size, String search,
                                                           String sort, String direction);
    AdminUserResponseDto getAdminUserDto(Long userId);
    AdminUserResponseDto createAdminUser(AdminCreateUserRequestDto request);
    AdminUserResponseDto updateAdminUser(Long userId, AdminUpdateUserRequestDto request);
    void deactivateAdminUser(Long userId);
}
