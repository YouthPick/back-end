package com.bop.youthpick.post.repository;

import com.bop.youthpick.post.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"user", "policy"})
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"user", "policy"})
    Page<Post> findAllByDeletedAtIsNull(Pageable pageable);
}
