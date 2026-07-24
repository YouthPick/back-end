package com.bop.youthpick.file.service;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    MinioClient minioClient(MinioProperties properties) {
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(properties.endpoint())
                        .credentials(properties.accessKey(), properties.secretKey())
                        .build();
        minioClient.setTimeout(
                properties.connectTimeout().toMillis(),
                properties.writeTimeout().toMillis(),
                properties.readTimeout().toMillis());
        return minioClient;
    }
}
