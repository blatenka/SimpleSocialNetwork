package com.aloute.service.impl;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.auth.response.CurrentUserResponseDto;
import com.aloute.dto.admin.response.AdminUserResponseDto;
import com.aloute.dto.admin.request.AdminCreateUserRequestDto;
import com.aloute.dto.admin.request.AdminUpdateUserRequestDto;
import com.aloute.dto.user.response.UserInfoResponseDto;
import com.aloute.dto.user.response.UserProfileResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.dto.user.request.UpdateUserInfoRequestDto;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.exception.BadRequestException;
import com.aloute.model.entity.User;
import com.aloute.model.entity.UserInfo;
import com.aloute.model.enums.Role;
import com.aloute.model.enums.UserStatus;
import com.aloute.repository.mysql.UserInfoRepository;
import com.aloute.repository.mysql.UserRepository;
import com.aloute.service.UserService;
import com.aloute.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.aloute.util.SecurityUtils;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserInfoRepository userInfoRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserInfo getUserProfile(Long userId) {
        return userInfoRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    @Override
    public List<UserInfo> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return userInfoRepository.findAll();
        }
        return userInfoRepository.findByFullnameContainingIgnoreCaseOrNicknameContainingIgnoreCase(keyword, keyword);
    }

    @Override
    public void lockUser(Long userId) {
        User user = getUserById(userId);
        ensureCanDeactivate(user);
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
    }

    @Override
    public void unlockUser(Long userId) {
        User user = getUserById(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public User getCurrentUser() {
        Long id = securityUtils.getCurrentUserId();
        return getUserById(id);
    }

    @Override
    public CurrentUserResponseDto getCurrentUserDto() {
        User user = getCurrentUser();
        return CurrentUserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }

    @Override
    public UserProfileResponseDto getUserProfileDto(Long userId) {
        return UserMapper.toProfileResponse(getUserById(userId), getUserProfile(userId));
    }

    @Override
    public UserInfoResponseDto updateCurrentProfile(Long requestedUserId, UpdateUserInfoRequestDto request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!currentUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        UserInfo existingInfo = getUserProfile(currentUserId);
        if (request.getFullname() != null) existingInfo.setFullname(request.getFullname());
        if (request.getNickname() != null) existingInfo.setNickname(request.getNickname());
        if (request.getDescription() != null) existingInfo.setDescription(request.getDescription());
        if (request.getDob() != null) existingInfo.setDob(request.getDob());
        if (request.getHometown() != null) existingInfo.setHometown(request.getHometown());
        if (request.getAvatarUrl() != null) existingInfo.setAvatarUrl(request.getAvatarUrl());
        if (request.getCoverUrl() != null) existingInfo.setCoverUrl(request.getCoverUrl());
        return UserMapper.toInfoResponse(userInfoRepository.save(existingInfo));
    }

    @Override
    public UserSummaryResponseDto getUserSummary(Long userId) {
        UserSummaryResponseDto summary = UserMapper.toSummary(getUserProfile(userId));
        summary.setEmail(getUserById(userId).getEmail());
        return summary;
    }

    @Override
    public List<UserSummaryResponseDto> getUserSummaries(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = new LinkedHashSet<>(userIds).stream().toList();
        Map<Long, UserSummaryResponseDto> summaries = userInfoRepository.findAllById(distinctIds).stream()
                .map(UserMapper::toSummary)
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, Function.identity()));
        Map<Long, String> emails = userRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return userIds.stream()
                .map(id -> {
                    UserSummaryResponseDto summary = summaries.get(id);
                    if (summary == null) throw new ResourceNotFoundException("User profile not found");
                    summary.setEmail(emails.get(id));
                    return summary;
                })
                .toList();
    }

    @Override
    public List<UserSummaryResponseDto> searchUserSummaries(String keyword) {
        return getUserSummaries(searchUsers(keyword).stream().map(UserInfo::getId).toList());
    }

    @Override
    public List<UserSummaryResponseDto> searchUserSummariesByFullname(String fullname) {
        return getUserSummaries(userInfoRepository.findByFullnameContainingIgnoreCase(fullname).stream()
                .map(UserInfo::getId).toList());
    }

    @Override
    public PageResponseDto<AdminUserResponseDto> getAdminUserDtos(int page, int size) {
        return getAdminUserDtos(page, size, null, "createdAt", "desc");
    }

    @Override
    public PageResponseDto<AdminUserResponseDto> getAdminUserDtos(
            int page, int size, String search, String sort, String direction) {
        String sortField = switch (sort == null ? "" : sort) {
            case "id" -> "id";
            case "email" -> "email";
            case "role" -> "role";
            case "status" -> "status";
            default -> "createdAt";
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String keyword = search == null || search.isBlank() ? null : search.trim();
        Page<User> users = userRepository.searchAdminUsers(keyword,
                PageRequest.of(page, size, Sort.by(sortDirection, sortField)));
        List<Long> ids = users.getContent().stream().map(User::getId).toList();
        Map<Long, UserInfo> infos = userInfoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserInfo::getId, Function.identity()));
        return PageResponseDto.from(users.map(user -> toAdminUser(user, infos.get(user.getId()))));
    }

    @Override
    public AdminUserResponseDto getAdminUserDto(Long userId) {
        User user = getUserById(userId);
        return toAdminUser(user, userInfoRepository.findById(userId).orElse(null));
    }

    @Override
    public AdminUserResponseDto createAdminUser(AdminCreateUserRequestDto request) {
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmail(email)) throw new BadRequestException("Email is already in use");
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        UserInfo info = UserInfo.builder()
                .user(user)
                .fullname(request.getFullname().trim())
                .nickname(request.getNickname() == null ? null : request.getNickname().trim())
                .build();
        user.setUserInfo(info);
        User saved = userRepository.save(user);
        return toAdminUser(saved, saved.getUserInfo());
    }

    @Override
    public AdminUserResponseDto updateAdminUser(Long userId, AdminUpdateUserRequestDto request) {
        User user = getUserById(userId);
        UserInfo info = getUserProfile(userId);
        if (request.getFullname() != null) info.setFullname(request.getFullname().trim());
        if (request.getNickname() != null) info.setNickname(request.getNickname().trim());
        if (request.getStatus() != null) {
            if (request.getStatus() == UserStatus.BANNED) ensureCanDeactivate(user);
            user.setStatus(request.getStatus());
            userRepository.save(user);
        }
        return toAdminUser(user, userInfoRepository.save(info));
    }

    @Override
    public void deactivateAdminUser(Long userId) {
        lockUser(userId);
    }

    private void ensureCanDeactivate(User user) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (user.getId().equals(currentUserId)) {
            throw new BadRequestException("You cannot deactivate your own administrator account");
        }
        if (user.getRole() == Role.ADMIN && user.getStatus() == UserStatus.ACTIVE
                && userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new BadRequestException("The last active administrator cannot be deactivated");
        }
    }

    private AdminUserResponseDto toAdminUser(User user, UserInfo info) {
        return AdminUserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole() == null ? null : user.getRole().name())
                .status(user.getStatus() == null ? null : user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .fullname(info == null ? null : info.getFullname())
                .nickname(info == null ? null : info.getNickname())
                .avatarUrl(info == null ? null : info.getAvatarUrl())
                .build();
    }

}