package com.aloute.mapper;

import com.aloute.dto.comment.response.CommentResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.document.Comment;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;

public final class CommentMapper {
    private CommentMapper() {}

    public static CommentResponseDto toResponse(Comment comment, UserSummaryResponseDto author,
                                                ReactionSummaryResponseDto reactionSummary) {
        if (comment == null) return null;
        return CommentResponseDto.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .parentId(comment.getParentId())
                .author(author)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .reactionSummary(reactionSummary)
                .build();
    }
}
