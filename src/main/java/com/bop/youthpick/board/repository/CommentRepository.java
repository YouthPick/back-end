package com.bop.youthpick.board.repository;

import com.bop.youthpick.board.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByPostId(Long postId);
}
