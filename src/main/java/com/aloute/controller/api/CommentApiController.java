package com.aloute.controller.api;

import com.aloute.dto.comment.request.CreateCommentRequestDto;
import com.aloute.dto.comment.response.CommentResponseDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.common.ApiMessageResponseDto;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.dto.reaction.response.ReactionUserResponseDto;
import com.aloute.model.enums.ReactionType;
import com.aloute.model.enums.TargetType;
import com.aloute.service.CommentService;
import com.aloute.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Validated
public class CommentApiController {

    private final CommentService commentService;
    private final ReactionService reactionService;

    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentResponseDto> addComment(
            @PathVariable String postId, @Valid @RequestBody CreateCommentRequestDto request) {
        CommentResponseDto c = commentService.addComment(postId, request.getContent());
        return ResponseEntity.ok(c);
    }

    @PostMapping("/reply/{parentId}")
    public ResponseEntity<CommentResponseDto> reply(
            @PathVariable String parentId, @Valid @RequestBody CreateCommentRequestDto request) {
        CommentResponseDto c = commentService.replyComment(parentId, request.getContent());
        return ResponseEntity.ok(c);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponseDto> delete(@PathVariable String id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Deleted").build());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<PageResponseDto<CommentResponseDto>> listByPost(@PathVariable String postId,
                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResponseDto<CommentResponseDto> comments = commentService.getCommentsByPost(postId, page, size);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/replies/{parentId}")
    public ResponseEntity<PageResponseDto<CommentResponseDto>> listReplies(@PathVariable String parentId,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResponseDto<CommentResponseDto> replies = commentService.getReplies(parentId, page, size);
        return ResponseEntity.ok(replies);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ReactionSummaryResponseDto> likeComment(
            @PathVariable String id, @RequestParam(defaultValue = "LIKE") ReactionType type) {
        return ResponseEntity.ok(reactionService.toggleReaction(id, TargetType.COMMENT, type));
    }

    @GetMapping("/{id}/reactions")
    public ResponseEntity<List<ReactionUserResponseDto>> commentReactions(@PathVariable String id) {
        return ResponseEntity.ok(reactionService.getReactors(id, TargetType.COMMENT));
    }
}
