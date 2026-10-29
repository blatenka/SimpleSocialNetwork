package com.aloute.service;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.post.response.PostResponseDto;
import com.aloute.model.enums.Privacy;

import java.util.List;

public interface PostService {
    PostResponseDto createPost(String content, List<String> mediaUrls, Privacy privacy);
    PostResponseDto editPost(String postId, String content, List<String> mediaUrls, Privacy privacy);
    void deletePost(String postId);
    void adminDeletePost(String postId);
    PostResponseDto getPostById(String postId);
    PageResponseDto<PostResponseDto> getNewsfeed(int page, int size);
    PageResponseDto<PostResponseDto> getUserWall(Long targetUserId, int page, int size);
    PageResponseDto<PostResponseDto> searchPosts(String keyword, int page, int size);
    PageResponseDto<PostResponseDto> getAllPosts(int page, int size);
}
