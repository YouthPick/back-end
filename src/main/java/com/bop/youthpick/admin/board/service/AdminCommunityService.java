package com.bop.youthpick.admin.board.service;

import com.bop.youthpick.admin.board.dto.AdminAttachmentResponse;
import com.bop.youthpick.admin.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.admin.board.dto.AdminCommunityPostResponse;
import com.bop.youthpick.admin.board.repository.AdminCommunityPostSpecifications;
import com.bop.youthpick.board.entity.Comment;
import com.bop.youthpick.board.entity.Post;
import com.bop.youthpick.board.exception.BoardErrorCode;
import com.bop.youthpick.board.exception.BoardException;
import com.bop.youthpick.board.repository.AttachmentRepository;
import com.bop.youthpick.board.repository.CommentRepository;
import com.bop.youthpick.board.repository.PostRepository;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
        Specification<Post> spec =
                AdminCommunityPostSpecifications.filter(category, authorId, startDate, endDate)
                        .and(fetchUser());
        return postRepository.findAll(spec, pageable).map(AdminCommunityPostResponse::from);
    }

    // 목록 페이지당(기본 20건) User 조회 N+1을 막기 위해 작성자를 함께 fetch한다.
    // count 쿼리(select type=Long)에는 fetch를 적용하지 않는다 — to-one 연관관계라 row 중복은 없다.
    private Specification<Post> fetchUser() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("user", JoinType.LEFT);
            }
            return cb.conjunction();
        };
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
