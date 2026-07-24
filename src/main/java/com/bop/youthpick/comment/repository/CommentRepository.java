package com.bop.youthpick.comment.repository;

import com.bop.youthpick.comment.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByPostId(Long postId);

    /**
     * 삭제되지 않은 댓글만 조회한다.
     *
     * <p>이 프로젝트는 댓글을 DB에서 진짜로 지우지 않고 deleted_at 컬럼에 삭제 시각만 기록한다(soft delete). 그래서 위의
     * findByPostId()는 삭제된 댓글까지 전부 가져온다. 관리자 화면은 삭제된 댓글도 봐야 하므로 그 메서드는 그대로 두고, 일반 사용자용 조회는 이 메서드를
     * 쓴다.
     *
     * <p>메서드 이름만 지어주면 Spring Data JPA가 SQL을 자동으로 만들어 준다. AndDeletedAtIsNull = "그리고 deleted_at 이
     * NULL인 것만"
     */
    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByPostIdAndDeletedAtIsNull(Long postId);
}
