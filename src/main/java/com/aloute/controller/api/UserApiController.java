package com.aloute.controller.api;

import com.aloute.dto.auth.response.CurrentUserResponseDto;
import com.aloute.dto.user.request.UpdateUserInfoRequestDto;
import com.aloute.dto.user.response.UserInfoResponseDto;
import com.aloute.dto.user.response.UserProfileResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserApiController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDto> me() {
        return ResponseEntity.ok(userService.getCurrentUserDto());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfileDto(id));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileResponseDto> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserProfileDto(id));
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserProfileResponseDto> getUserByEmail(
            @RequestParam @NotBlank @Email @Size(max = 100) String email) {
        UserProfileResponseDto profile = userService.getUserProfileDto(
                userService.getUserByEmail(email).getId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserInfoResponseDto> updateProfile(
            @PathVariable Long id, @Valid @RequestBody UpdateUserInfoRequestDto request) {
        return ResponseEntity.ok(userService.updateCurrentProfile(id, request));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryResponseDto>> search(
            @RequestParam @NotBlank @Size(max = 100) String keyword) {
        return ResponseEntity.ok(userService.searchUserSummaries(keyword));
    }

    @GetMapping("/search-by-fullname")
    public ResponseEntity<List<UserSummaryResponseDto>> searchByFullname(
            @RequestParam @NotBlank @Size(max = 100) String fullname) {
        return ResponseEntity.ok(userService.searchUserSummariesByFullname(fullname));
    }
}
