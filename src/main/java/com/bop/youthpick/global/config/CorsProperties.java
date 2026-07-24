package com.bop.youthpick.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youthpick.cors")
public record CorsProperties(List<String> allowedOrigins) {}
