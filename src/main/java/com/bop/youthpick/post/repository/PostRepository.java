package com.bop.youthpick.post.repository;

import com.bop.youthpick.post.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @EntityGraph(attributePaths = {"user", "policy"}) // jpql을 써서 구현하는 방법도 있음
    Optional<Post> findByIdAndDeletedAtIsNull(Long id); // 쿼리메서드

    @EntityGraph(attributePaths = {"user", "policy"})
    Page<Post> findAllByDeletedAtIsNull(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(
            "update Post p set p.viewCount = p.viewCount + 1 where p.id = :id and p.deletedAt is null")
    int incrementViewCount(@Param("id") Long id);
}
