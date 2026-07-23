package com.bop.youthpick.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.comment.dto.CommentCreateRequest;
import com.bop.youthpick.comment.dto.CommentResponse;
import com.bop.youthpick.comment.dto.CommentUpdateRequest;
import com.bop.youthpick.comment.entity.Comment;
import com.bop.youthpick.comment.exception.CommentErrorCode;
import com.bop.youthpick.comment.exception.CommentException;
import com.bop.youthpick.comment.repository.CommentRepository;
import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.exception.BoardErrorCode;
import com.bop.youthpick.post.exception.BoardException;
import com.bop.youthpick.post.repository.PostRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository, userRepository);
    }

    @Test
    void 최상위_댓글을_생성한다() {
        Post post = post(1L);
        User user = user(1L);
        CommentCreateRequest request = new CommentCreateRequest("내용", null);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CommentResponse response = commentService.create(1L, 1L, request);

        assertThat(response.parentId()).isNull();
        assertThat(response.content()).isEqualTo("내용");
        verify(commentRepository, never()).findById(any());
    }

    @Test
    void 대댓글을_생성하면_parentId가_설정된다() {
        Post post = post(1L);
        User user = user(1L);
        Comment parent = comment(10L, post, user(2L), null);
        CommentCreateRequest request = new CommentCreateRequest("답글", 10L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parent));

        CommentResponse response = commentService.create(1L, 1L, request);

        assertThat(response.parentId()).isEqualTo(10L);
    }

    @Test
    void 존재하지_않는_게시글에_댓글을_달면_POST_NOT_FOUND_예외가_발생한다() {
        CommentCreateRequest request = new CommentCreateRequest("내용", null);
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(1L, 1L, request))
                .isInstanceOf(BoardException.class)
                .extracting(exception -> ((BoardException) exception).getErrorCode())
                .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 존재하지_않는_부모_댓글에_대댓글을_달면_COMMENT_NOT_FOUND_예외가_발생한다() {
        Post post = post(1L);
        User user = user(1L);
        CommentCreateRequest request = new CommentCreateRequest("답글", 99L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(1L, 1L, request))
                .isInstanceOf(CommentException.class)
                .extracting(exception -> ((CommentException) exception).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void 대댓글에_다시_답글을_달면_REPLY_DEPTH_EXCEEDED_예외가_발생한다() {
        Post post = post(1L);
        User user = user(1L);
        Comment grandParent = comment(5L, post, user(2L), null);
        Comment parentReply = comment(10L, post, user(2L), grandParent);
        CommentCreateRequest request = new CommentCreateRequest("답글의 답글", 10L);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(parentReply));

        assertThatThrownBy(() -> commentService.create(1L, 1L, request))
                .isInstanceOf(CommentException.class)
                .extracting(exception -> ((CommentException) exception).getErrorCode())
                .isEqualTo(CommentErrorCode.REPLY_DEPTH_EXCEEDED);
    }

    @Test
    void 작성자_본인이_댓글을_수정한다() {
        Comment comment = comment(3L, post(1L), user(1L), null);
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");
        when(commentRepository.findById(3L)).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.update(1L, 3L, request);

        assertThat(response.content()).isEqualTo("수정된 내용");
    }

    @Test
    void 다른_사용자는_댓글을_수정할_수_없다() {
        Comment comment = comment(3L, post(1L), user(1L), null);
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");
        when(commentRepository.findById(3L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(2L, 3L, request))
                .isInstanceOf(CommentException.class)
                .extracting(exception -> ((CommentException) exception).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_ACCESS_DENIED);
    }

    @Test
    void 존재하지_않는_댓글을_수정하면_COMMENT_NOT_FOUND_예외가_발생한다() {
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");
        when(commentRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(1L, 3L, request))
                .isInstanceOf(CommentException.class)
                .extracting(exception -> ((CommentException) exception).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void 작성자_본인이_댓글을_삭제한다() {
        Comment comment = comment(3L, post(1L), user(1L), null);
        when(commentRepository.findById(3L)).thenReturn(Optional.of(comment));

        commentService.delete(1L, 3L);

        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    void 다른_사용자는_댓글을_삭제할_수_없다() {
        Comment comment = comment(3L, post(1L), user(1L), null);
        when(commentRepository.findById(3L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(2L, 3L))
                .isInstanceOf(CommentException.class)
                .extracting(exception -> ((CommentException) exception).getErrorCode())
                .isEqualTo(CommentErrorCode.COMMENT_ACCESS_DENIED);
    }

    @Test
    void 게시글의_댓글_목록을_조회한다() {
        Post post = post(1L);
        Comment comment = comment(3L, post, user(1L), null);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdAndDeletedAtIsNull(1L)).thenReturn(List.of(comment));

        var response = commentService.getAllByPostId(1L);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().content()).isEqualTo("내용");
    }

    @Test
    void 존재하지_않는_게시글의_댓글을_조회하면_POST_NOT_FOUND_예외가_발생한다() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getAllByPostId(1L))
                .isInstanceOf(BoardException.class)
                .extracting(exception -> ((BoardException) exception).getErrorCode())
                .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }

    private Post post(Long id) {
        Post post = mock(Post.class);
        lenient().when(post.getId()).thenReturn(id);
        return post;
    }

    private User user(Long id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Comment comment(Long id, Post post, User user, Comment parent) {
        Comment comment = Comment.create(post, user, parent, "내용");
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
