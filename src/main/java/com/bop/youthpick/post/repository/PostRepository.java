package com.bop.youthpick.post.repository;

import com.bop.youthpick.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @EntityGraph(attributePaths = {"user", "policy"}) // jpql을 써서 구현하는 방법도 있음
    Optional<Post> findByIdAndDeletedAtIsNull(Long id); // 쿼리메서드

    // sitemap.xml 생성 전용 — 목록/상세 API처럼 user/policy까지 끌고 올 필요가 없어 id/updatedAt만 projection한다.
    List<PostSitemapView> findAllByDeletedAtIsNullOrderByUpdatedAtDesc();

    interface PostSitemapView {
        Long getId();

        LocalDateTime getUpdatedAt();
    }

    // JpaSpecificationExecutor의 기본 findAll(Specification, Pageable)을 재선언해
    // @EntityGraph로 목록 조회 시 user/policy N+1을 막는다(PostSummaryResponse가 둘 다 참조).
    @Override
    @EntityGraph(attributePaths = {"user", "policy"})
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(
            "update Post p set p.viewCount = p.viewCount + 1 where p.id = :id and p.deletedAt is null")
    int incrementViewCount(@Param("id") Long id);
}
