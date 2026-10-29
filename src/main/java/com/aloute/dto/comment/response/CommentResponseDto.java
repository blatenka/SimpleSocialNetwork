package com.aloute.dto.comment.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private String id;
    private String postId;
    private String parentId;
    private UserSummaryResponseDto author;
    private String content;
    private LocalDateTime createdAt;
    private ReactionSummaryResponseDto reactionSummary;
    private long replyCount;
}
