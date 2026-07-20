package com.bop.youthpick.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.board.repository.AttachmentRepository;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.post.dto.PostCreateRequest;
import com.bop.youthpick.post.dto.PostDetailResponse;
import com.bop.youthpick.post.dto.PostSummaryResponse;
import com.bop.youthpick.post.dto.PostUpdateRequest;
import com.bop.youthpick.post.entity.Attachment;
import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.entity.PostCategory;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private PostViewLogStore postViewLogStore;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService =
                new PostService(
                        postRepository,
                        attachmentRepository,
                        userRepository,
                        policyRepository,
                        postViewLogStore);
    }

    @Test
    void 자유글을_생성한다() {
        User user = user(1L);
        PostCreateRequest request = new PostCreateRequest("FREE", "제목", "내용", null, null);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PostDetailResponse response = postService.create(1L, request);

        assertThat(response.category()).isEqualTo("FREE");
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.policyId()).isNull();
        verify(policyRepository, never()).findById(any());
    }

    @Test
    void 게시글을_생성할_때_업로드된_이미지를_첨부로_저장한다() {
        User user = user(1L);
        PostCreateRequest request =
                new PostCreateRequest(
                        "FREE",
                        "이미지 글",
                        "<img src=\"/api/v1/files/2e5c2c2f-22c7-43a9-8d2c-8902a29b2b21\">",
                        null,
                        List.of(
                                "/api/v1/files/2e5c2c2f-22c7-43a9-8d2c-8902a29b2b21",
                                "/api/v1/files/2e5c2c2f-22c7-43a9-8d2c-8902a29b2b21"));
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        postService.create(1L, request);

        verify(attachmentRepository)
                .saveAll(
                        org.mockito.ArgumentMatchers.argThat(
                                attachments -> {
                                    assertThat(attachments)
                                            .extracting(Attachment::getFileUrl)
                                            .containsExactly(
                                                    "/api/v1/files/2e5c2c2f-22c7-43a9-8d2c-8902a29b2b21");
                                    return true;
                                }));
    }

    @Test
    void 후기를_생성할_때_정책을_연결한다() {
        User user = user(1L);
        Policy policy = policy(10L, "청년 정책");
        PostCreateRequest request = new PostCreateRequest("REVIEW", "후기", "내용", 10L, null);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(policyRepository.findById(10L)).thenReturn(Optional.of(policy));
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PostDetailResponse response = postService.create(1L, request);

        assertThat(response.category()).isEqualTo("REVIEW");
        assertThat(response.policyId()).isEqualTo(10L);
        assertThat(response.policyTitle()).isEqualTo("청년 정책");
    }

    @Test
    void 삭제되지_않은_게시글_목록을_조회한다() {
        Post post = Post.create(user(1L), null, PostCategory.FREE, "제목", "내용");
        PageRequest pageable = PageRequest.of(0, 20);
        when(postRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(post), pageable, 1));

        Page<PostSummaryResponse> result = postService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().title()).isEqualTo("제목");
    }

    @Test
    void 처음_조회할_때_게시글_상세를_조회하고_조회수를_증가시킨다() {
        Post post = Post.create(user(1L), null, PostCategory.FREE, "제목", "내용");
        when(postViewLogStore.isFirstView(3L, "user:1")).thenReturn(true);
        when(postRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(post));

        PostDetailResponse response = postService.findById(3L, 1L, "127.0.0.1");

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        verify(postRepository).incrementViewCount(3L);
    }

    @Test
    void 이미_조회한_경우_조회수를_증가시키지_않고_상세를_조회한다() {
        Post post = Post.create(user(1L), null, PostCategory.FREE, "제목", "내용");
        when(postViewLogStore.isFirstView(3L, "ip:127.0.0.1")).thenReturn(false);
        when(postRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(post));

        PostDetailResponse response = postService.findById(3L, null, "127.0.0.1");

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        verify(postRepository, never()).incrementViewCount(3L);
    }

    @Test
    void 작성자가_게시글을_수정하면_기존_이미지_첨부를_최신_목록으로_교체한다() {
        Post post = Post.create(user(1L), null, PostCategory.FREE, "수정 전", "수정 전 내용");
        PostUpdateRequest request =
                new PostUpdateRequest(
                        "FREE",
                        "수정 후",
                        "수정 후 내용",
                        null,
                        List.of("/api/v1/files/2e5c2c2f-22c7-43a9-8d2c-8902a29b2b21"));
        when(postRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(post));

        PostDetailResponse response = postService.update(1L, 3L, request);

        assertThat(response.title()).isEqualTo("수정 후");
        assertThat(response.content()).isEqualTo("수정 후 내용");
        verify(attachmentRepository).deleteByPostId(3L);
        verify(attachmentRepository).saveAll(any());
    }

    @Test
    void 작성자가_게시글을_soft_delete한다() {
        Post post = Post.create(user(1L), null, PostCategory.FREE, "제목", "내용");
        when(postRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(post));

        postService.delete(1L, 3L);

        assertThat(post.getDeletedAt()).isNotNull();
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    void 다른_사용자는_게시글을_수정할_수_없다() {
        Post post = Post.create(user(1L), null, PostCategory.FREE, "제목", "내용");
        PostUpdateRequest request = new PostUpdateRequest("FREE", "수정", "수정", null, null);
        when(postRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(2L, 3L, request))
                .isInstanceOf(BoardException.class)
                .extracting(exception -> ((BoardException) exception).getErrorCode())
                .isEqualTo(BoardErrorCode.POST_ACCESS_DENIED);
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private Policy policy(Long id, String title) {
        Policy policy = mock(Policy.class);
        when(policy.getId()).thenReturn(id);
        when(policy.getTitle()).thenReturn(title);
        return policy;
    }
}
