package com.bop.youthpick.board.repository;

import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.entity.PostCategory;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 게시글 목록 조회의 선택적 필터(category/authorId/createdAt 범위)를 조합한다. */
public final class AdminCommunityPostSpecifications {

    private AdminCommunityPostSpecifications() {}

    public static Specification<Post> filter(
            String category, Long authorId, LocalDate startDate, LocalDate endDate) {
        return Specification.allOf(
                categoryEquals(category),
                authorIdEquals(authorId),
                createdBetween(startDate, endDate));
    }

    private static Specification<Post> categoryEquals(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        PostCategory value = PostCategory.valueOf(category);
        return (root, query, cb) -> cb.equal(root.get("category"), value);
    }

    private static Specification<Post> authorIdEquals(Long authorId) {
        if (authorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), authorId);
    }

    /** createdAt이 [startDate, endDate] 날짜 범위 안에 있는 게시글만 남긴다(KST 변환 없이 직접 비교). */
    private static Specification<Post> createdBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (startDate != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(
                        cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
