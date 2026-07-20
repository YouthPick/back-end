package com.bop.youthpick.board.repository;

import com.bop.youthpick.post.entity.Attachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByPostId(Long postId);

    void deleteByPostId(Long postId);
}
