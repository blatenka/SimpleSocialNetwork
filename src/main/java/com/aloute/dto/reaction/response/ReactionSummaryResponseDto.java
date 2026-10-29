package com.aloute.dto.reaction.response;

import com.aloute.model.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionSummaryResponseDto {
    private Map<String, Long> counts;
    private ReactionType currentUserReaction;
}
