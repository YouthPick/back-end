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
        User user = findUser(userId); // userId는 그냥 맘대로 정한 변수명? 아니면 컬럼명을 카멜케이스로 쓴거? findUser는 쿼리메서드?
        PostCategory category = PostCategory.valueOf(request.category());
        // valueOf()는 모든 enum이 제공받는 정적 메서드. 문자열과 이름이 일치하는 enum 값을 찾아줌

        Policy policy = resolvePolicy(category, request.policyId());
        Post post = Post.create(user, policy, category, request.title(), request.content());
        // request record로 선언됐으면 거기 안에 있는 변수들을 메서드로 쓰면 자동으로 똑같이 반환해줌
        return PostDetailResponse.from(postRepository.save(post));
        // from도 누가준 선물인지 모르겠어.. 암튼 위에서 받은 Post객체를 레포지토리에 저장해서 디테일리스폰스 객체로 보내준다는뜻같음
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
    } // PostDetailRequest가 record라면 Java가 이 메서드들(category() 등)을 자동으로 만듦

    @Transactional
    public void delete(Long userId, Long postId) {
        Post post = findPost(postId);
        validateAuthor(post, userId);
        post.softDelete();
    }

    private User findUser(Long userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
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
                return policyRepository
                        .findById(policyId)
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));
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
