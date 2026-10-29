package com.aloute.dto.reaction.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.model.enums.ReactionType;
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
public class ReactionUserResponseDto {
    private UserSummaryResponseDto author;
    private ReactionType reactionType;
}
