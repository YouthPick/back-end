package com.bop.youthpick.sitemap.service;

import com.bop.youthpick.global.config.SiteProperties;
import com.bop.youthpick.post.repository.PostRepository;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 검색엔진(Search Console)에 등록할 sitemap.xml을 요청마다 생성한다. 프론트는 SSR 없는 Vite SPA라 빌드 시점에는 게시글처럼 동적인 URL을 알
 * 수 없으므로, 백엔드가 DB를 조회해 매 요청 최신 목록으로 만들어 낸다.
 */
@Service
@RequiredArgsConstructor
public class SitemapService {

    private static final DateTimeFormatter LASTMOD_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PostRepository postRepository;
    private final SiteProperties siteProperties;

    @Transactional(readOnly = true)
    public String generate() {
        String baseUrl = siteProperties.frontendUrl();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, baseUrl + "/", null, "daily", "1.0");
        appendUrl(xml, baseUrl + "/search", null, "daily", "0.8");
        appendUrl(xml, baseUrl + "/community", null, "hourly", "0.8");

        for (PostRepository.PostSitemapView post :
                postRepository.findAllByDeletedAtIsNullOrderByUpdatedAtDesc()) {
            String lastmod = LASTMOD_FORMATTER.format(post.getUpdatedAt());
            appendUrl(xml, baseUrl + "/community/" + post.getId(), lastmod, "weekly", "0.6");
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(
            StringBuilder xml, String loc, String lastmod, String changefreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
