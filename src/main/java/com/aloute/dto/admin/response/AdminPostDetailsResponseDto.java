package com.aloute.dto.admin.response;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.comment.response.CommentResponseDto;
import com.aloute.dto.post.response.PostResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPostDetailsResponseDto {
    private PostResponseDto post;
    private PageResponseDto<CommentResponseDto> comments;
}
