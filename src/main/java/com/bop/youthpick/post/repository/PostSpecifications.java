package com.bop.youthpick.post.repository;

import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.entity.PostCategory;
import org.springframework.data.jpa.domain.Specification;

/** 게시글 목록 조회의 선택적 필터(category/검색어)를 조합한다. */
public final class PostSpecifications {

    private PostSpecifications() {}

    public static Specification<Post> search(String category, String query) {
        return Specification.allOf(
                notDeleted(), categoryEquals(category), titleOrContentContains(query));
    }

    private static Specification<Post> notDeleted() {
        return (root, cq, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Post> categoryEquals(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        PostCategory value = PostCategory.valueOf(category);
        return (root, cq, cb) -> cb.equal(root.get("category"), value);
    }

    private static Specification<Post> titleOrContentContains(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, cq, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern));
    }
}
