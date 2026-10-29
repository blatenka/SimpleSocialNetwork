package com.aloute.dto.admin.response;

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
public class AdminStatisticsResponseDto {
    private long totalPosts;
    private long postsLastWeek;
    private long postsLastMonth;
    private long totalComments;
    private long commentsLastWeek;
    private long commentsLastMonth;
    private long totalReactions;
    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;
    private long usersLastWeek;
    private long usersLastMonth;
    private long activePosts;
    private long totalReplies;
    private long acceptedFriendships;
    private long pendingFriendRequests;
    private String mysqlHealth;
    private String mongoHealth;
}
