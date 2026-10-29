package com.aloute.mapper;

import com.aloute.dto.post.response.PostResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.document.Post;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;

public final class PostMapper {
    private PostMapper() {}

    public static PostResponseDto toResponse(Post post, UserSummaryResponseDto author,
                                             ReactionSummaryResponseDto reactionSummary) {
        if (post == null) return null;
        return PostResponseDto.builder()
                .id(post.getId())
                .author(author)
                .content(post.getContent())
                .mediaUrls(post.getMediaUrls())
                .privacy(post.getPrivacy())
                .createdAt(post.getCreatedAt())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .reactionSummary(reactionSummary)
                .build();
    }
}
