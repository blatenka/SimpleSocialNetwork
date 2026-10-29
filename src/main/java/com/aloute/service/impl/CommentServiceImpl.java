package com.aloute.service.impl;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.comment.response.CommentResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.mapper.CommentMapper;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.exception.UnauthorizedException;
import com.aloute.model.document.Comment;
import com.aloute.model.document.Post;
import com.aloute.model.enums.NotificationType;
import com.aloute.model.enums.TargetType;
import com.aloute.model.entity.UserInfo;
import com.aloute.service.UserService;
import com.aloute.repository.mongo.CommentRepository;
import com.aloute.repository.mongo.PostRepository;
import com.aloute.service.CommentService;
import com.aloute.service.NotificationService;
import com.aloute.service.ReactionService;
import com.aloute.service.FriendshipService;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.exception.BadRequestException;
import com.aloute.model.enums.Privacy;
import com.aloute.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ReactionService reactionService;
    private final FriendshipService friendshipService;

    @Override
    public CommentResponseDto addComment(String postId, String content) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        ensureCanView(post);
        content = validatedContent(content);
        Comment comment = Comment.builder()
                .postId(postId)
                .userId(currentUserId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        Comment saved = commentRepository.save(comment);
        incrementCommentCount(postId, 1);

        if (!post.getUserId().equals(currentUserId)) {
            try {
                String actorName = "Someone";
                try { UserInfo ui = userService.getUserProfile(currentUserId); if (ui.getFullname() != null) actorName = ui.getFullname(); } catch (Exception ignored) {}
                notificationService.createNotification(post.getUserId(), currentUserId,
                        NotificationType.COMMENT_POST, saved.getId(), TargetType.COMMENT,
                        actorName + " đã bình luận bài viết của bạn", "/posts/" + postId);
            } catch (Exception ignored) {
            }
        }
        UserSummaryResponseDto author = userService.getUserSummary(currentUserId);
        return CommentMapper.toResponse(saved, author, reactionService.getSummaries(
                List.of(saved.getId()), TargetType.COMMENT).get(saved.getId()));
    }

    @Override
    public CommentResponseDto replyComment(String parentCommentId, String content) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bình luận không tồn tại"));
        Post post = postRepository.findById(parent.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        ensureCanView(post);
        content = validatedContent(content);
        Comment reply = Comment.builder()
                .postId(parent.getPostId())
                .userId(currentUserId)
                .parentId(parentCommentId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        Comment saved = commentRepository.save(reply);
        incrementCommentCount(parent.getPostId(), 1);

        if (!parent.getUserId().equals(currentUserId)) {
            try {
                String actorName = "Someone";
                try { UserInfo ui = userService.getUserProfile(currentUserId); if (ui.getFullname() != null) actorName = ui.getFullname(); } catch (Exception ignored) {}
                notificationService.createNotification(parent.getUserId(), currentUserId,
                        NotificationType.REPLY_COMMENT, saved.getId(), TargetType.COMMENT,
                        actorName + " đã trả lời bình luận của bạn", "/posts/" + parent.getPostId());
            } catch (Exception ignored) {
            }
        }
        UserSummaryResponseDto author = userService.getUserSummary(currentUserId);
        return CommentMapper.toResponse(saved, author, reactionService.getSummaries(
                List.of(saved.getId()), TargetType.COMMENT).get(saved.getId()));
    }

    @Override
    public void deleteComment(String commentId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bình luận không tồn tại"));
        if (!comment.getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("Không có quyền xoá bình luận này");
        }
        commentRepository.delete(comment);
        incrementCommentCount(comment.getPostId(), -1);
    }

    @Override
    public PageResponseDto<CommentResponseDto> getCommentsByPost(String postId, int page, int size) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        ensureCanView(post);
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId, pr);
        return mapResponses(comments);
    }

    @Override
    public PageResponseDto<CommentResponseDto> getReplies(String parentCommentId, int page, int size) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bình luận không tồn tại"));
        Post post = postRepository.findById(parent.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        ensureCanView(post);
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parentCommentId, pr);
        return mapResponses(replies);
    }

    private PageResponseDto<CommentResponseDto> mapResponses(Page<Comment> comments) {
        List<Long> authorIds = comments.getContent().stream().map(Comment::getUserId).distinct().toList();
        Map<Long, UserSummaryResponseDto> authors = userService.getUserSummaries(authorIds).stream()
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, Function.identity()));
        Map<String, ReactionSummaryResponseDto> summaries = reactionService.getSummaries(
                comments.getContent().stream().map(Comment::getId).toList(), TargetType.COMMENT);
        List<String> commentIds = comments.getContent().stream().map(Comment::getId).toList();
        Map<String, Long> replyCounts = commentIds.isEmpty() ? Map.of()
                : commentRepository.findReplyParentReferences(commentIds).stream()
                .map(Comment::getParentId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return PageResponseDto.from(comments.map(comment -> {
            CommentResponseDto response = CommentMapper.toResponse(
                    comment, authors.get(comment.getUserId()), summaries.get(comment.getId()));
            response.setReplyCount(replyCounts.getOrDefault(comment.getId(), 0L));
            return response;
        }));
    }

    private String validatedContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty() || normalized.length() > 2000) {
            throw new BadRequestException("Comment must contain 1 to 2000 characters");
        }
        return normalized;
    }

    private void ensureCanView(Post post) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (post.getUserId().equals(currentUserId) || post.getPrivacy() == null
                || post.getPrivacy() == Privacy.PUBLIC) return;
        if (post.getPrivacy() == Privacy.FRIENDS
                && friendshipService.areFriends(post.getUserId(), currentUserId)) return;
        throw new AccessDeniedException("You cannot interact with this post");
    }

    private void incrementCommentCount(String postId, int delta) {
        Query q = new Query(Criteria.where("id").is(postId));
        Update u = new Update().inc("commentCount", delta);
        mongoTemplate.updateFirst(q, u, Post.class);
    }
}