package com.aloute.service.impl;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.post.response.PostResponseDto;
import com.aloute.dto.user.response.UserSummaryResponseDto;
import com.aloute.mapper.PostMapper;
import com.aloute.service.UserService;
import com.aloute.service.ReactionService;
import com.aloute.service.FriendshipService;
import com.aloute.exception.ResourceNotFoundException;
import com.aloute.exception.UnauthorizedException;
import com.aloute.model.document.Comment;
import com.aloute.model.document.Post;
import com.aloute.model.enums.Privacy;
import com.aloute.model.enums.TargetType;
import org.springframework.security.access.AccessDeniedException;
import com.aloute.dto.reaction.response.ReactionSummaryResponseDto;
import com.aloute.repository.mongo.PostRepository;
import com.aloute.repository.mongo.CommentRepository;
import com.aloute.repository.mongo.ReactionRepository;
import com.aloute.service.PostService;
import com.aloute.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final SecurityUtils securityUtils;
    private final UserService userService;
    private final ReactionService reactionService;
    private final FriendshipService friendshipService;
    private final MongoTemplate mongoTemplate;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;

    @Override
    public PostResponseDto createPost(String content, List<String> mediaUrls, Privacy privacy) {
        Long currentUserId = securityUtils.getCurrentUserId();

        Post post = Post.builder()
                .userId(currentUserId)
                .content(content)
                .mediaUrls(mediaUrls != null ? mediaUrls : List.of())
                .privacy(privacy != null ? privacy : Privacy.PUBLIC)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .likeCount(0)
                .commentCount(0)
                .shareCount(0)
                .build();

        Post saved = postRepository.save(post);
        UserSummaryResponseDto author = userService.getUserSummary(currentUserId);
        return toResponse(saved, author, reactionService.getSummaries(List.of(saved.getId()), TargetType.POST)
                .get(saved.getId()));
    }

    @Override
    public PostResponseDto editPost(String postId, String content, List<String> mediaUrls, Privacy privacy) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new ResourceNotFoundException("Bài viết không tồn tại");
        }
        if (!post.getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("Bạn không có quyền sửa bài viết này");
        }
        if (content != null) post.setContent(content);
        if (mediaUrls != null) post.setMediaUrls(mediaUrls);
        if (privacy != null) post.setPrivacy(privacy);
        Post saved = postRepository.save(post);
        UserSummaryResponseDto author = userService.getUserSummary(post.getUserId());
        return toResponse(saved, author, reactionService.getSummaries(List.of(saved.getId()), TargetType.POST)
                .get(saved.getId()));
    }

    @Override
    public void deletePost(String postId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new ResourceNotFoundException("Bài viết không tồn tại");
        }
        if (!post.getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("Bạn không có quyền xoá bài viết này");
        }
        post.setIsDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    public void adminDeletePost(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new ResourceNotFoundException("Bài viết không tồn tại");
        }
        List<String> commentIds = commentRepository.findIdsByPostId(postId).stream()
                .map(Comment::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        reactionRepository.deleteByTargetIdAndTargetType(postId, TargetType.POST);
        if (!commentIds.isEmpty()) {
            reactionRepository.deleteByTargetTypeAndTargetIdIn(TargetType.COMMENT, commentIds);
        }
        commentRepository.deleteByPostId(postId);
        post.setIsDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    public PostResponseDto getPostById(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại"));
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new ResourceNotFoundException("Bài viết không tồn tại");
        }
        ensureCanView(post);
        UserSummaryResponseDto author = userService.getUserSummary(post.getUserId());
        return toResponse(post, author, reactionService.getSummaries(List.of(post.getId()), TargetType.POST)
                .get(post.getId()));
    }

    @Override
    public PageResponseDto<PostResponseDto> getNewsfeed(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = findVisiblePosts(pageRequest, null, null);
        return mapResponses(posts);
    }

    @Override
    public PageResponseDto<PostResponseDto> getUserWall(Long targetUserId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = findVisiblePosts(pageRequest, null, targetUserId);
        return mapResponses(posts);
    }

    @Override
    public PageResponseDto<PostResponseDto> searchPosts(String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = findVisiblePosts(pageRequest,
                keyword == null || keyword.isBlank() ? null : keyword, null);
        return mapResponses(posts);
    }

    @Override
    public PageResponseDto<PostResponseDto> getAllPosts(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findByIsDeletedFalse(pageRequest);
        return mapResponses(posts);
    }

    private PageResponseDto<PostResponseDto> mapResponses(Page<Post> posts) {
        List<Long> authorIds = posts.getContent().stream().map(Post::getUserId).distinct().toList();
        Map<Long, UserSummaryResponseDto> authors = userService.getUserSummaries(authorIds).stream()
                .collect(Collectors.toMap(UserSummaryResponseDto::getId, Function.identity()));
        Map<String, ReactionSummaryResponseDto> summaries = reactionService.getSummaries(
                posts.getContent().stream().map(Post::getId).toList(), TargetType.POST);
        return PageResponseDto.from(posts.map(post ->
                toResponse(post, authors.get(post.getUserId()), summaries.get(post.getId()))));
    }

    private PostResponseDto toResponse(Post post, UserSummaryResponseDto author,
                                       ReactionSummaryResponseDto reactionSummary) {
        return PostMapper.toResponse(post, author, reactionSummary);
    }

    private Page<Post> findVisiblePosts(PageRequest pageRequest, String keyword, Long targetUserId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Long> visibleUserIds = new ArrayList<>(friendshipService.getFriendIds(currentUserId));
        visibleUserIds.add(currentUserId);
        Criteria visibility = new Criteria().orOperator(
                Criteria.where("privacy").is(Privacy.PUBLIC),
                Criteria.where("privacy").is(null),
                Criteria.where("userId").is(currentUserId),
                new Criteria().andOperator(
                        Criteria.where("privacy").is(Privacy.FRIENDS),
                        Criteria.where("userId").in(visibleUserIds)));
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("isDeleted").is(false));
        filters.add(visibility);
        if (targetUserId != null) filters.add(Criteria.where("userId").is(targetUserId));
        if (keyword != null) filters.add(Criteria.where("content").regex(
                java.util.regex.Pattern.quote(keyword), "i"));
        Criteria criteria = new Criteria().andOperator(filters.toArray(Criteria[]::new));
        Query query = Query.query(criteria).with(pageRequest);
        List<Post> content = mongoTemplate.find(query, Post.class);
        long total = mongoTemplate.count(Query.query(criteria), Post.class);
        return new org.springframework.data.domain.PageImpl<>(content, pageRequest, total);
    }

    private void ensureCanView(Post post) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (post.getUserId().equals(currentUserId) || post.getPrivacy() == null
                || post.getPrivacy() == Privacy.PUBLIC) return;
        if (post.getPrivacy() == Privacy.FRIENDS
                && friendshipService.areFriends(post.getUserId(), currentUserId)) return;
        throw new AccessDeniedException("You cannot view this post");
    }
}