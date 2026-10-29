package com.aloute.controller.api;

import com.aloute.dto.chat.request.SendMessageRequestDto;
import com.aloute.dto.chat.response.MessageResponseDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.common.CountResponseDto;
import com.aloute.dto.common.ApiMessageResponseDto;
import com.aloute.dto.chat.response.ConversationResponseDto;
import com.aloute.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Validated
public class ChatApiController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponseDto>> conversations() {
        return ResponseEntity.ok(chatService.getConversations());
    }

    @PostMapping("/send")
    public ResponseEntity<MessageResponseDto> send(@Valid @RequestBody SendMessageRequestDto request) {
        MessageResponseDto msg = chatService.sendMessage(
                request.getRecipientId(), request.getContent(), request.getMediaUrl());
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/history/{otherUserId}")
    public ResponseEntity<PageResponseDto<MessageResponseDto>> history(@PathVariable Long otherUserId,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageResponseDto<MessageResponseDto> msgs = chatService.getHistory(otherUserId, page, size);
        return ResponseEntity.ok(msgs);
    }

    @PostMapping("/read/{otherUserId}")
    public ResponseEntity<ApiMessageResponseDto> read(@PathVariable Long otherUserId) {
        chatService.markAsRead(otherUserId);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("OK").build());
    }

    @GetMapping("/unread/{otherUserId}")
    public ResponseEntity<CountResponseDto> unread(@PathVariable Long otherUserId) {
        return ResponseEntity.ok(CountResponseDto.builder()
                .count(chatService.getUnreadCount(otherUserId))
                .build());
    }
}