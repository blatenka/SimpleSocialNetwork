package com.aloute.controller.api;

import com.aloute.dto.post.request.CreatePostRequestDto;
import com.aloute.dto.post.request.UpdatePostRequestDto;
import com.aloute.dto.post.response.PostResponseDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.common.ApiMessageResponseDto;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.dto.reaction.response.ReactionUserResponseDto;
import com.aloute.model.enums.Privacy;
import com.aloute.model.enums.ReactionType;
import com.aloute.service.PostService;
import com.aloute.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
public class PostApiController {

    private final PostService postService;
    private final ReactionService reactionService;

    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(@Valid @RequestBody CreatePostRequestDto request) {
        Privacy privacy = request.getPrivacy() != null ? request.getPrivacy() : Privacy.PUBLIC;
        PostResponseDto post = postService.createPost(request.getContent(), request.getMediaUrls(), privacy);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> editPost(
            @PathVariable String id, @Valid @RequestBody UpdatePostRequestDto request) {
        Privacy privacy = request.getPrivacy(); // null allowed
        PostResponseDto post = postService.editPost(id, request.getContent(), request.getMediaUrls(), privacy);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponseDto> deletePost(@PathVariable String id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiMessageResponseDto.builder().message("Deleted").build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable String id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @GetMapping("/newsfeed")
    public ResponseEntity<PageResponseDto<PostResponseDto>> newsfeed(@RequestParam(defaultValue = "0") @Min(0) int page,
                                      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResponseDto<PostResponseDto> posts = postService.getNewsfeed(page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponseDto<PostResponseDto>> userWall(@PathVariable Long userId,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResponseDto<PostResponseDto> posts = postService.getUserWall(userId, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponseDto<PostResponseDto>> search(
                                    @RequestParam @NotBlank @Size(max = 100) String keyword,
                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResponseDto<PostResponseDto> posts = postService.searchPosts(keyword, page, size);
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ReactionSummaryResponseDto> likePost(
            @PathVariable String id, @RequestParam(defaultValue = "LIKE") ReactionType type) {
        return ResponseEntity.ok(reactionService.toggleReaction(id, com.aloute.model.enums.TargetType.POST, type));
    }

    @GetMapping("/{id}/reactions")
    public ResponseEntity<List<ReactionUserResponseDto>> postReactions(@PathVariable String id) {
        return ResponseEntity.ok(reactionService.getReactors(id, com.aloute.model.enums.TargetType.POST));
    }
}
