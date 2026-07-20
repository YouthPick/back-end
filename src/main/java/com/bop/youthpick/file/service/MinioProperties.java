package com.bop.youthpick.file.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youthpick.minio")
public record MinioProperties(String endpoint, String accessKey, String secretKey, String bucket) {}
