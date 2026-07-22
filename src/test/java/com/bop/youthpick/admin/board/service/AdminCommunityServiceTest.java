package com.bop.youthpick.admin.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.post.entity.Comment;
import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.exception.BoardErrorCode;
import com.bop.youthpick.post.exception.BoardException;
import com.bop.youthpick.post.repository.AttachmentRepository;
import com.bop.youthpick.post.repository.CommentRepository;
import com.bop.youthpick.post.repository.PostRepository;
import com.bop.youthpick.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminCommunityServiceTest {

    @Mock private PostRepository postRepository;

    @Mock private CommentRepository commentRepository;

    @Mock private AttachmentRepository attachmentRepository;

    private AdminCommunityService adminCommunityService;

    private static final Long POST_ID = 1L;
    private static final Long COMMENT_ID = 2L;

    @BeforeEach
    void setUp() {
        adminCommunityService =
                new AdminCommunityService(postRepository, commentRepository, attachmentRepository);
    }

    private Post mockPost() {
        Post post = mock(Post.class);
        User user = mock(User.class);
        when(post.getId()).thenReturn(POST_ID);
        when(post.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(10L);
        when(user.getNickname()).thenReturn("닉네임");
        return post;
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_페이지를_반환한다() {
        Page<Post> page = new PageImpl<>(List.of(mockPost()));
        when(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<?> result =
                adminCommunityService.searchPosts("QUESTION", 10L, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 댓글_목록을_조회한다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(mock(Post.class)));
        Comment comment = mock(Comment.class);
        Post post = mock(Post.class);
        User user = mock(User.class);
        when(comment.getId()).thenReturn(COMMENT_ID);
        when(comment.getPost()).thenReturn(post);
        when(post.getId()).thenReturn(POST_ID);
        when(comment.getUser()).thenReturn(user);
        when(user.getNickname()).thenReturn("닉네임");
        when(comment.getContent()).thenReturn("좋은 정책이네요");
        when(commentRepository.findByPostId(POST_ID)).thenReturn(List.of(comment));

        List<?> result = adminCommunityService.getComments(POST_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void 댓글_조회_대상_게시글이_없으면_POST_NOT_FOUND_예외를_던진다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.getComments(POST_ID))
                .isInstanceOf(BoardException.class)
                .extracting(ex -> ((BoardException) ex).getErrorCode())
                .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 첨부파일_조회_대상_게시글이_없으면_POST_NOT_FOUND_예외를_던진다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.getAttachments(POST_ID))
                .isInstanceOf(BoardException.class)
                .extracting(ex -> ((BoardException) ex).getErrorCode())
                .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 게시글을_soft_delete_한다() {
        Post post = mockPost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        adminCommunityService.deletePost(POST_ID);

        verify(post).softDelete();
    }

    @Test
    void 게시글_삭제_대상이_없으면_POST_NOT_FOUND_예외를_던진다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.deletePost(POST_ID))
                .isInstanceOf(BoardException.class)
                .extracting(ex -> ((BoardException) ex).getErrorCode())
                .isEqualTo(BoardErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 댓글을_soft_delete_한다() {
        Comment comment = mock(Comment.class);
        Post post = mock(Post.class);
        User user = mock(User.class);
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(comment.getId()).thenReturn(COMMENT_ID);
        when(comment.getPost()).thenReturn(post);
        when(post.getId()).thenReturn(POST_ID);
        when(comment.getUser()).thenReturn(user);
        when(user.getNickname()).thenReturn("닉네임");

        adminCommunityService.deleteComment(COMMENT_ID);

        verify(comment).softDelete();
    }

    @Test
    void 댓글_삭제_대상이_없으면_COMMENT_NOT_FOUND_예외를_던진다() {
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCommunityService.deleteComment(COMMENT_ID))
                .isInstanceOf(BoardException.class)
                .extracting(ex -> ((BoardException) ex).getErrorCode())
                .isEqualTo(BoardErrorCode.COMMENT_NOT_FOUND);
    }
}
