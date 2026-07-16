package com.bop.youthpick.admin.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.board.entity.Post;
import com.bop.youthpick.board.entity.PostCategory;
import com.bop.youthpick.board.repository.PostRepository;
import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminCommunityPostSpecificationsTest {

    @Autowired private PostRepository postRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private TestEntityManager entityManager;

    private Long authorId;

    @BeforeEach
    void setUp() {
        User author = userRepository.save(User.createSocialUser("kakao", "kakao-1", null, "닉네임"));
        User otherAuthor =
                userRepository.save(User.createSocialUser("naver", "naver-1", null, "다른닉네임"));
        authorId = author.getId();

        savePost(author, PostCategory.QUESTION, "정책 질문", LocalDateTime.of(2026, 3, 15, 9, 0));
        savePost(author, PostCategory.REVIEW, "정책 후기", LocalDateTime.of(2026, 6, 1, 9, 0));
        savePost(otherAuthor, PostCategory.FREE, "잡담", LocalDateTime.of(2026, 6, 10, 9, 0));
        entityManager.clear();
    }

    // createdAt은 @CreatedDate + updatable=false라 auditing이 현재 시각으로 채운 뒤에는
    // JPA로 값을 바꿀 수 없다. 날짜 필터 테스트를 위해 네이티브 쿼리로 직접 덮어쓴다.
    private void savePost(
            User author, PostCategory category, String title, LocalDateTime createdAt) {
        Post post = BeanUtils.instantiateClass(Post.class);
        ReflectionTestUtils.setField(post, "user", author);
        ReflectionTestUtils.setField(post, "category", category);
        ReflectionTestUtils.setField(post, "title", title);
        ReflectionTestUtils.setField(post, "content", "내용");
        postRepository.saveAndFlush(post);

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE posts SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, post.getId())
                .executeUpdate();
    }

    @Test
    void category로_필터링한다() {
        List<Post> result =
                postRepository.findAll(
                        AdminCommunityPostSpecifications.filter("QUESTION", null, null, null));

        assertThat(result).extracting(Post::getTitle).containsExactly("정책 질문");
    }

    @Test
    void authorId로_필터링한다() {
        List<Post> result =
                postRepository.findAll(
                        AdminCommunityPostSpecifications.filter(null, authorId, null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void createdAt_날짜범위로_필터링한다() {
        List<Post> result =
                postRepository.findAll(
                        AdminCommunityPostSpecifications.filter(
                                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));

        assertThat(result).extracting(Post::getTitle).containsExactly("정책 질문");
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<Post> result =
                postRepository.findAll(
                        AdminCommunityPostSpecifications.filter(null, null, null, null));

        assertThat(result).hasSize(3);
    }
}
