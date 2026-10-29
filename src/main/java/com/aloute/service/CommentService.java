package com.aloute.service;

import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.comment.response.CommentResponseDto;

public interface CommentService {
    CommentResponseDto addComment(String postId, String content);
    CommentResponseDto replyComment(String parentCommentId, String content);
    void deleteComment(String commentId);
    PageResponseDto<CommentResponseDto> getCommentsByPost(String postId, int page, int size);
    PageResponseDto<CommentResponseDto> getReplies(String parentCommentId, int page, int size);
}
