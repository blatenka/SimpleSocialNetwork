package com.aloute.dto.post.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.model.enums.Privacy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private String id;
    private UserSummaryResponseDto author;
    private String content;
    private List<String> mediaUrls;
    private Privacy privacy;
    private LocalDateTime createdAt;
    private int likeCount;
    private int commentCount;
    private int shareCount;
    private ReactionSummaryResponseDto reactionSummary;
}
