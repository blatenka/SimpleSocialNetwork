package com.aloute.service;

import com.aloute.dto.admin.response.AdminStatisticsResponseDto;
import com.aloute.dto.admin.response.AdminPostDetailsResponseDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.post.response.PostResponseDto;

public interface AdminService {
    AdminStatisticsResponseDto getStatistics();
    PageResponseDto<PostResponseDto> getPosts(int page, int size, String search, String sort, String direction);
    AdminPostDetailsResponseDto getPostDetails(String postId, int page, int size);
}
