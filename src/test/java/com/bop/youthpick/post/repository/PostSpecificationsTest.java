package com.bop.youthpick.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.entity.PostCategory;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostSpecificationsTest {

    @Autowired private PostRepository postRepository;

    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User author = userRepository.save(User.createSocialUser("kakao", "kakao-1", null, "닉네임"));
        postRepository.save(Post.create(author, null, PostCategory.QUESTION, "정책 질문 있어요", "궁금합니다"));
        postRepository.save(Post.create(author, null, PostCategory.REVIEW, "정책 후기", "도움이 됐어요"));
        postRepository.save(Post.create(author, null, PostCategory.FREE, "잡담", "오늘 날씨 좋네요"));
    }

    @Test
    void category로_필터링한다() {
        List<Post> result = postRepository.findAll(PostSpecifications.search("QUESTION", null));

        assertThat(result).extracting(Post::getTitle).containsExactly("정책 질문 있어요");
    }

    @Test
    void 제목에_검색어가_포함된_게시글을_찾는다() {
        List<Post> result = postRepository.findAll(PostSpecifications.search(null, "질문"));

        assertThat(result).extracting(Post::getTitle).containsExactly("정책 질문 있어요");
    }

    @Test
    void 본문에_검색어가_포함된_게시글도_찾는다() {
        List<Post> result = postRepository.findAll(PostSpecifications.search(null, "날씨"));

        assertThat(result).extracting(Post::getTitle).containsExactly("잡담");
    }

    @Test
    void 검색어는_대소문자를_구분하지_않는다() {
        postRepository.save(
                Post.create(
                        userRepository.findAll().getFirst(),
                        null,
                        PostCategory.FREE,
                        "Hello World",
                        "내용"));

        List<Post> result = postRepository.findAll(PostSpecifications.search(null, "HELLO"));

        assertThat(result).extracting(Post::getTitle).containsExactly("Hello World");
    }

    @Test
    void 카테고리와_검색어를_함께_적용한다() {
        List<Post> result = postRepository.findAll(PostSpecifications.search("FREE", "잡담"));

        assertThat(result).extracting(Post::getTitle).containsExactly("잡담");
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<Post> result = postRepository.findAll(PostSpecifications.search(null, null));

        assertThat(result).hasSize(3);
    }
}
