package com.aloute.service;

import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.model.enums.ReactionType;
import com.aloute.model.enums.TargetType;
import com.aloute.dto.reaction.response.ReactionUserResponseDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ReactionService {
    ReactionSummaryResponseDto toggleReaction(String targetId, TargetType targetType, ReactionType reactionType);
    Map<String, ReactionSummaryResponseDto> getSummaries(Collection<String> targetIds, TargetType targetType);
    List<ReactionUserResponseDto> getReactors(String targetId, TargetType targetType);
}