package com.bop.youthpick.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.user.entity.User;
import org.junit.jupiter.api.Test;

class PostTest {

    @Test
    void 게시글을_생성한다() {
        User user = mock(User.class);
        Policy policy = mock(Policy.class);

        Post post = Post.create(user, policy, PostCategory.REVIEW, "후기", "정책 참여 후기입니다.");

        assertThat(post.getUser()).isSameAs(user);
        assertThat(post.getPolicy()).isSameAs(policy);
        assertThat(post.getCategory()).isEqualTo(PostCategory.REVIEW);
        assertThat(post.getTitle()).isEqualTo("후기");
        assertThat(post.getContent()).isEqualTo("정책 참여 후기입니다.");
        assertThat(post.getViewCount()).isZero();
        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    void 게시글_내용을_수정한다() {
        Post post = Post.create(mock(User.class), null, PostCategory.FREE, "수정 전", "수정 전 내용");
        Policy policy = mock(Policy.class);

        post.update(policy, PostCategory.QUESTION, "수정 후", "수정 후 내용");

        assertThat(post.getPolicy()).isSameAs(policy);
        assertThat(post.getCategory()).isEqualTo(PostCategory.QUESTION);
        assertThat(post.getTitle()).isEqualTo("수정 후");
        assertThat(post.getContent()).isEqualTo("수정 후 내용");
    }

    @Test
    void 게시글을_soft_delete한다() {
        Post post = Post.create(mock(User.class), null, PostCategory.FREE, "제목", "내용");

        post.softDelete();

        assertThat(post.getDeletedAt()).isNotNull();
    }
}
