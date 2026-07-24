package com.bop.youthpick.sitemap.controller;

import com.bop.youthpick.sitemap.service.SitemapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * sitemap.xml은 URL 프로토콜 특성상 사이트 루트("/sitemap.xml")에서 서빙해야 하므로 다른 컨트롤러처럼 {@code /api/v1} 아래 두지 않는다.
 * nginx가 이 경로를 백엔드로 프록시한다(front-end/nginx.conf 참고).
 */
@Tag(name = "SEO")
@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final SitemapService sitemapService;

    @Operation(
            summary = "sitemap.xml 조회",
            description = "정적 페이지 + 공개 커뮤니티 게시글 URL을 담은 sitemap을 반환한다.")
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok(sitemapService.generate());
    }
}
