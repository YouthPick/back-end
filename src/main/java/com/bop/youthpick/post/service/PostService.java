package com.bop.youthpick.post.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.post.dto.PostCreateRequest;
import com.bop.youthpick.post.dto.PostDetailResponse;
import com.bop.youthpick.post.dto.PostSummaryResponse;
import com.bop.youthpick.post.dto.PostUpdateRequest;
import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.entity.PostCategory;
import com.bop.youthpick.post.exception.BoardErrorCode;
import com.bop.youthpick.post.exception.BoardException;
import com.bop.youthpick.post.repository.PostRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;

    @Transactional
    public PostDetailResponse create(Long userId, PostCreateRequest request) {
        User user = findUser(userId);
        PostCategory category = PostCategory.valueOf(request.category());
        Policy policy = resolvePolicy(category, request.policyId());
        Post post = Post.create(user, policy, category, request.title(), request.content());
        return PostDetailResponse.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> findAll(Pageable pageable) {
        return postRepository.findAllByDeletedAtIsNull(pageable).map(PostSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public PostDetailResponse findById(Long postId) {
        return PostDetailResponse.from(findPost(postId));
    }

    @Transactional
    public PostDetailResponse update(Long userId, Long postId, PostUpdateRequest request) {
        Post post = findPost(postId);
        validateAuthor(post, userId);
        PostCategory category = PostCategory.valueOf(request.category());
        Policy policy = resolvePolicy(category, request.policyId());
        post.update(policy, category, request.title(), request.content());
        return PostDetailResponse.from(post);
    }

    private User findUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
    }

    private Post findPost(Long postId) {
        return postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));
    }

    private Policy resolvePolicy(PostCategory category, Long policyId) {
        if (category == PostCategory.FREE) {
            if (policyId != null) {
                throw new BoardException(BoardErrorCode.FREE_POST_POLICY_NOT_ALLOWED);
            }
            return null;
        }
        if (policyId == null) {
            throw new BoardException(BoardErrorCode.POLICY_REQUIRED);
        }
        return policyRepository
                .findById(policyId)
                .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));
    }

    private void validateAuthor(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new BoardException(BoardErrorCode.POST_ACCESS_DENIED);
        }
    }
}
