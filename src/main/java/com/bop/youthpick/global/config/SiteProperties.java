package com.bop.youthpick.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 프론트엔드 배포 도메인. sitemap.xml처럼 절대 URL을 만들어야 하는 곳에서 쓴다. */
@ConfigurationProperties(prefix = "youthpick.site")
public record SiteProperties(String frontendUrl) {}
