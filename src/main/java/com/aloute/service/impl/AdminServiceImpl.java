package com.aloute.service.impl;

import com.aloute.dto.admin.response.AdminPostDetailsResponseDto;
import com.aloute.dto.admin.response.AdminStatisticsResponseDto;
import com.aloute.dto.comment.response.CommentResponseDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.post.response.PostResponseDto;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.mapper.CommentMapper;
import com.aloute.mapper.PostMapper;
import com.aloute.model.document.Comment;
import com.aloute.model.document.Post;
import com.aloute.model.enums.FriendshipStatus;
import com.aloute.model.enums.TargetType;
import com.aloute.repository.mongo.CommentRepository;
import com.aloute.repository.mongo.PostRepository;
import com.aloute.repository.mongo.ReactionRepository;
import com.aloute.repository.mysql.FriendshipRepository;
import com.aloute.repository.mysql.UserRepository;
import com.aloute.service.AdminService;
import com.aloute.service.ReactionService;
import com.aloute.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserService userService;
    private final ReactionService reactionService;
    private final MongoTemplate mongoTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public AdminStatisticsResponseDto getStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime monthAgo = now.minusDays(30);
        String mysqlHealth = mysqlHealth();
        String mongoHealth = mongoHealth();

        return AdminStatisticsResponseDto.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(com.aloute.model.enums.UserStatus.ACTIVE))
                .bannedUsers(userRepository.countByStatus(com.aloute.model.enums.UserStatus.BANNED))
                .usersLastWeek(userRepository.countByCreatedAtBetween(weekAgo, now))
                .usersLastMonth(userRepository.countByCreatedAtBetween(monthAgo, now))
                .activePosts(postRepository.countByIsDeletedFalse())
                .totalPosts(postRepository.count())
                .postsLastWeek(postRepository.countByCreatedAtBetweenAndIsDeletedFalse(weekAgo, now))
                .postsLastMonth(postRepository.countByCreatedAtBetweenAndIsDeletedFalse(monthAgo, now))
                .totalComments(commentRepository.count())
                .totalReplies(commentRepository.countByParentIdIsNotNull())
                .commentsLastWeek(commentRepository.countByCreatedAtBetween(weekAgo, now))
                .commentsLastMonth(commentRepository.countByCreatedAtBetween(monthAgo, now))
                .totalReactions(reactionRepository.count())
                .acceptedFriendships(friendshipRepository.countByStatus(FriendshipStatus.ACCEPTED))
                .pendingFriendRequests(friendshipRepository.countByStatus(FriendshipStatus.PENDING))
                .mysqlHealth(mysqlHealth)
                .mongoHealth(mongoHealth)
                .build();
    }

    @Override
    public PageResponseDto<PostResponseDto> getPosts(
            int page, int size, String search, String sort, String direction) {
        String sortField = switch (sort == null ? "" : sort) {
            case "likeCount" -> "likeCount";
            case "commentCount" -> "commentCount";
            case "content" -> "content";
            default -> "createdAt";
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Criteria criteria = Criteria.where("isDeleted").ne(true);
        if (search != null && !search.isBlank()) {
            criteria = new Criteria().andOperator(
                    criteria,
                    Criteria.where("content").regex(Pattern.quote(search.trim()), "i"));
        }
        Query pageQuery = Query.query(criteria).with(pageRequest);
        List<Post> content = mongoTemplate.find(pageQuery, Post.class);
        long total = mongoTemplate.count(Query.query(criteria), Post.class);
        return mapPosts(new PageImpl<>(content, pageRequest, total));
    }

    @Override
    public AdminPostDetailsResponseDto getPostDetails(String postId, int page, int size) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        Page<Comment> commentPage = commentRepository.findByPostIdOrderByCreatedAtDesc(postId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Long> userIds = commentPage.getContent().stream().map(Comment::getUserId).distinct().toList();
        Map<Long, UserSummaryResponseDto> authors = userService.getUserSummaries(userIds).stream()
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, Function.identity()));
        Map<String, ReactionSummaryResponseDto> commentReactions = reactionService.getSummaries(
                commentPage.getContent().stream().map(Comment::getId).toList(), TargetType.COMMENT);
        Page<CommentResponseDto> comments = commentPage
                .map(comment -> CommentMapper.toResponse(comment, authors.get(comment.getUserId()),
                        commentReactions.get(comment.getId())));
        PostResponseDto postDto = mapPosts(new PageImpl<>(List.of(post), PageRequest.of(0, 1), 1))
                .getContent().get(0);
        return AdminPostDetailsResponseDto.builder()
                .post(postDto)
                .comments(PageResponseDto.from(comments))
                .build();
    }

    private PageResponseDto<PostResponseDto> mapPosts(Page<Post> posts) {
        List<Long> authorIds = posts.getContent().stream().map(Post::getUserId).distinct().toList();
        Map<Long, UserSummaryResponseDto> authors = userService.getUserSummaries(authorIds).stream()
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, Function.identity()));
        Map<String, ReactionSummaryResponseDto> summaries = reactionService.getSummaries(
                posts.getContent().stream().map(Post::getId).toList(), TargetType.POST);
        return PageResponseDto.from(posts.map(post ->
                PostMapper.toResponse(post, authors.get(post.getUserId()), summaries.get(post.getId()))));
    }

    private String mysqlHealth() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (DataAccessException exception) {
            logger.warn("Admin MySQL health check failed", exception);
            return "DOWN";
        }
    }

    private String mongoHealth() {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return "UP";
        } catch (DataAccessException exception) {
            logger.warn("Admin MongoDB health check failed", exception);
            return "DOWN";
        }
    }
}
