package com.aloute.dto.friendship.response;

import com.aloute.dto.user.response.UserSummaryResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MutualFriendsResponseDto {
    private long count;
    private List<UserSummaryResponseDto> friends;
}
