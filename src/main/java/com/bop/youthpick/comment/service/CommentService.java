package com.bop.youthpick.comment.service;

import com.bop.youthpick.comment.dto.CommentCreateRequest;
import com.bop.youthpick.comment.dto.CommentResponse;
import com.bop.youthpick.comment.dto.CommentUpdateRequest;
import com.bop.youthpick.comment.entity.Comment;
import com.bop.youthpick.comment.exception.CommentErrorCode;
import com.bop.youthpick.comment.exception.CommentException;
import com.bop.youthpick.comment.repository.CommentRepository;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.exception.BoardErrorCode;
import com.bop.youthpick.post.exception.BoardException;
import com.bop.youthpick.post.repository.PostRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse create(Long uid, Long pid, CommentCreateRequest req) {
        Post post =
                postRepository
                        .findById(pid)
                        .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));

        User user =
                userRepository
                        .findById(uid)
                        .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));

        Comment parent = resolveParent(req.parentId(), pid);

        Comment comment = Comment.create(post, user, parent, req.content());

        commentRepository.save(comment);

        return CommentResponse.from(comment);
    }

    // parentId가 있으면 대댓글이다. 이 프로젝트의 댓글은 1단까지만 허용한다(Comment.java 클래스 주석 참고) —
    // 부모 댓글이 이미 다른 댓글의 대댓글이면 답글의 답글이 되므로 막는다.
    private Comment resolveParent(Long parentId, Long postId) {
        if (parentId == null) {
            return null;
        }

        Comment parent =
                commentRepository
                        .findById(parentId)
                        .orElseThrow(
                                () -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (!parent.getPost().getId().equals(postId)) {
            throw new CommentException(CommentErrorCode.COMMENT_NOT_FOUND);
        }

        if (parent.getParent() != null) {
            throw new CommentException(CommentErrorCode.REPLY_DEPTH_EXCEEDED);
        }

        return parent;
    }

    /** 댓글을 수정하는 메서드 */
    @Transactional
    public CommentResponse update(Long uid, Long commentId, CommentUpdateRequest req) {
        Comment comment =
                commentRepository
                        .findById(commentId)
                        .orElseThrow(
                                () -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(uid)) {
            throw new CommentException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }

        comment.updateContent(req.content());

        return CommentResponse.from(comment);
    }

    /** 댓글을 삭제하는 메서드 */
    @Transactional
    public void delete(Long uid, Long commentId) {
        // TODO: 1. id를 통해 댓글을 조회한다. -Post id 가 아니라 comment id
        Comment comment =
                commentRepository
                        .findById(commentId)

                        // TODO: 2. 댓글이 존재하지 않으면 404 에러를 낸다.
                        .orElseThrow(
                                () -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        // [추가] 3. 작성자 본인인지 확인한다.
        // 이 검사가 없으면 로그인만 한 사람은 누구나 남의 댓글을 지울 수 있다.
        // uid 는 JWT 토큰에서 꺼낸 "지금 요청한 사람"의 id 이고,
        // comment.getUser().getId() 는 "이 댓글을 쓴 사람"의 id 다. 둘이 같아야 삭제를 허용한다.
        //
        // Long 은 int 같은 기본 타입이 아니라 객체라서 == 로 비교하면
        // 값이 같아도 다르다고 나올 수 있다. 그래서 반드시 .equals() 를 쓴다.
        if (!comment.getUser().getId().equals(uid)) {
            throw new CommentException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }

        comment.softDelete();
    }

    /** 해당 포스트에 대한 댓글 전체 조회 메서드 */
    @Transactional(readOnly = true)
    public ApiResponse<List<CommentResponse>> getAllByPostId(Long pId) {
        postRepository
                .findById(pId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));

        // TODO: 3. 해당 포스트에 존재하는 댓글들을 List로 조회한다.
        // [수정] findByPostId -> findByPostIdAndDeletedAtIsNull
        // 삭제된 댓글은 deleted_at 에 시각만 기록되고 DB에 그대로 남아 있어서,
        // findByPostId 를 쓰면 이미 지운 댓글까지 목록에 나온다. 삭제 안 된 것만 가져오도록 바꿨다.
        List<Comment> comment = commentRepository.findByPostIdAndDeletedAtIsNull(pId);

        // TODO: 4. 조회한 결과들을 CommentResponse 타입으로 바꾼다.
        List<CommentResponse> convertResult =
                comment.stream()
                        .map(
                                oneDbLine -> {
                                    CommentResponse commentResponse =
                                            CommentResponse.from(oneDbLine);
                                    return commentResponse;
                                })
                        .toList();
        //            comment.stream().map(CommentResponse::from)

        // TODO: 5. 결과를 반환한다.
        return ApiResponse.ok(convertResult);
    }
}
