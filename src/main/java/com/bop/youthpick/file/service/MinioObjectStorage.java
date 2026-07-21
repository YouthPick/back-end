package com.bop.youthpick.file.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private volatile boolean bucketReady;

    @Override
    public void put(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType,
            Map<String, String> metadata)
            throws ObjectStorageException {
        try {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(properties.bucket()).object(objectKey).stream(
                                    inputStream, size, -1L)
                            .contentType(contentType)
                            .userMetadata(metadata)
                            .build());
        } catch (MinioException | IllegalArgumentException exception) {
            throw ObjectStorageException.unavailable(exception);
        }
    }

    @Override
    public StoredObject get(String objectKey) throws ObjectStorageException {
        try {
            StatObjectResponse stat =
                    minioClient.statObject(
                            StatObjectArgs.builder()
                                    .bucket(properties.bucket())
                                    .object(objectKey)
                                    .build());
            GetObjectResponse response =
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(properties.bucket())
                                    .object(objectKey)
                                    .build());
            String originalFilename = stat.userMetadata().getFirst("original-filename");
            Map<String, String> metadata =
                    originalFilename == null
                            ? Collections.emptyMap()
                            : Map.of("original-filename", originalFilename);
            return new StoredObject(response, stat.size(), stat.contentType(), metadata);
        } catch (ErrorResponseException exception) {
            if ("NoSuchKey".equals(exception.errorResponse().code())) {
                throw ObjectStorageException.objectNotFound(exception);
            }
            throw ObjectStorageException.unavailable(exception);
        } catch (MinioException | IllegalArgumentException exception) {
            throw ObjectStorageException.unavailable(exception);
        }
    }

    private synchronized void ensureBucket() throws MinioException {
        if (bucketReady) {
            return;
        }
        boolean exists =
                minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(properties.bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        }
        bucketReady = true;
    }
}
