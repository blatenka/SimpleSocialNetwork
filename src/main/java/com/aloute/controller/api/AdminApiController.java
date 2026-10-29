package com.aloute.controller.api;

import com.aloute.dto.admin.response.AdminUserResponseDto;
import com.aloute.dto.admin.response.AdminStatisticsResponseDto;
import com.aloute.dto.admin.response.AdminPostDetailsResponseDto;
import com.aloute.dto.admin.request.AdminCreateUserRequestDto;
import com.aloute.dto.admin.request.AdminUpdateUserRequestDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.common.ApiMessageResponseDto;
import com.aloute.dto.post.response.PostResponseDto;
import com.aloute.service.AdminService;
import com.aloute.service.PostService;
import com.aloute.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class AdminApiController {

    private final UserService userService;
    private final PostService postService;
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<PageResponseDto<AdminUserResponseDto>> users(
                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                   @RequestParam(required = false) @Size(max = 100) String search,
                                   @RequestParam(defaultValue = "createdAt") @Size(max = 30) String sort,
                                   @RequestParam(defaultValue = "desc") String direction) {
        PageResponseDto<AdminUserResponseDto> users =
                userService.getAdminUserDtos(page, size, search, sort, direction);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponseDto> user(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getAdminUserDto(id));
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponseDto> createUser(
            @Valid @RequestBody AdminCreateUserRequestDto request) {
        return ResponseEntity.ok(userService.createAdminUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserResponseDto> updateUser(
            @PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequestDto request) {
        return ResponseEntity.ok(userService.updateAdminUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiMessageResponseDto> deactivateUser(@PathVariable Long id) {
        userService.deactivateAdminUser(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Deactivated").build());
    }

    @PostMapping("/users/{id}/lock")
    public ResponseEntity<ApiMessageResponseDto> lock(@PathVariable Long id) {
        userService.lockUser(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Locked").build());
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<ApiMessageResponseDto> unlock(@PathVariable Long id) {
        userService.unlockUser(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Unlocked").build());
    }

    @GetMapping("/posts")
    public ResponseEntity<PageResponseDto<PostResponseDto>> posts(
                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                  @RequestParam(required = false) @Size(max = 100) String search,
                                  @RequestParam(defaultValue = "createdAt") @Size(max = 30) String sort,
                                  @RequestParam(defaultValue = "desc") String direction) {
        PageResponseDto<PostResponseDto> posts = adminService.getPosts(page, size, search, sort, direction);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<AdminPostDetailsResponseDto> postDetails(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(adminService.getPostDetails(id, page, size));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiMessageResponseDto> deletePost(@PathVariable String id) {
        postService.adminDeletePost(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Deleted").build());
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminStatisticsResponseDto> analytics() {
        return ResponseEntity.ok(adminService.getStatistics());
    }
}