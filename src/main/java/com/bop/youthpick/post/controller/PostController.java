package com.bop.youthpick.post.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.post.dto.PostCreateRequest;
import com.bop.youthpick.post.dto.PostDetailResponse;
import com.bop.youthpick.post.dto.PostSummaryResponse;
import com.bop.youthpick.post.dto.PostUpdateRequest;
import com.bop.youthpick.post.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostDetailResponse>> create(
            @CurrentUser Long userId, @Valid @RequestBody PostCreateRequest request) {
        PostDetailResponse response = postService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ApiResponse<List<PostSummaryResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        Page<PostSummaryResponse> page = postService.findAll(pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> findById(@PathVariable Long postId) {
        return ApiResponse.ok(postService.findById(postId));
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostDetailResponse> update(
            @CurrentUser Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request) {
        return ApiResponse.ok(postService.update(userId, postId, request));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(@CurrentUser Long userId, @PathVariable Long postId) {
        postService.delete(userId, postId);
        return ApiResponse.ok(null);
    }
}
