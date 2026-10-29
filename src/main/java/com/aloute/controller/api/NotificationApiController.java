package com.aloute.controller.api;

import com.aloute.service.NotificationService;
import com.aloute.service.UserService;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.common.CountResponseDto;
import com.aloute.dto.common.ApiMessageResponseDto;
import com.aloute.dto.notification.response.NotificationResponseDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationApiController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<NotificationResponseDto>> list(
                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Long current = userService.getCurrentUserDto().getId();
        PageResponseDto<NotificationResponseDto> n = notificationService.getNotifications(current, page, size);
        return ResponseEntity.ok(n);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CountResponseDto> unreadCount() {
        Long current = userService.getCurrentUserDto().getId();
        return ResponseEntity.ok(CountResponseDto.builder()
                .count(notificationService.getUnreadCount(current))
                .build());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiMessageResponseDto> markRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("OK").build());
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiMessageResponseDto> markAllRead() {
        Long current = userService.getCurrentUserDto().getId();
        notificationService.markAllAsRead(current);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("OK").build());
    }
}