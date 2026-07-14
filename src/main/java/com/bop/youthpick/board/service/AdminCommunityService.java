package com.bop.youthpick.board.service;

import com.bop.youthpick.board.dto.AdminAttachmentResponse;
import com.bop.youthpick.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.board.dto.AdminCommunityPostResponse;
import com.bop.youthpick.board.entity.Comment;
import com.bop.youthpick.board.entity.Post;
import com.bop.youthpick.board.exception.BoardErrorCode;
import com.bop.youthpick.board.exception.BoardException;
import com.bop.youthpick.board.repository.AdminCommunityPostSpecifications;
import com.bop.youthpick.board.repository.AttachmentRepository;
import com.bop.youthpick.board.repository.CommentRepository;
import com.bop.youthpick.board.repository.PostRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;

    @Transactional(readOnly = true)
    public Page<AdminCommunityPostResponse> searchPosts(
            String category,
            Long authorId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        return postRepository
                .findAll(
                        AdminCommunityPostSpecifications.filter(
                                category, authorId, startDate, endDate),
                        pageable)
                .map(AdminCommunityPostResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AdminCommunityCommentResponse> getComments(Long postId) {
        findPost(postId);
        return commentRepository.findByPostId(postId).stream()
                .map(AdminCommunityCommentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminAttachmentResponse> getAttachments(Long postId) {
        findPost(postId);
        return attachmentRepository.findByPostId(postId).stream()
                .map(AdminAttachmentResponse::from)
                .toList();
    }

    @Transactional
    public AdminCommunityPostResponse deletePost(Long postId) {
        Post post = findPost(postId);
        post.softDelete();
        return AdminCommunityPostResponse.from(post);
    }

    @Transactional
    public AdminCommunityCommentResponse deleteComment(Long commentId) {
        Comment comment = findComment(commentId);
        comment.softDelete();
        return AdminCommunityCommentResponse.from(comment);
    }

    private Post findPost(Long postId) {
        return postRepository
                .findById(postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));
    }

    private Comment findComment(Long commentId) {
        return commentRepository
                .findById(commentId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
    }
}
