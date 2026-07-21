package com.bop.youthpick.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.minio.GetObjectResponse;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinioObjectStorageTest {

    @Mock private MinioClient minioClient;

    private MinioObjectStorage objectStorage;

    @BeforeEach
    void setUp() {
        MinioProperties properties =
                new MinioProperties(
                        "http://localhost:9000",
                        "test-access-key",
                        "test-secret-key",
                        "test-bucket",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(30));
        objectStorage = new MinioObjectStorage(minioClient, properties);
    }

    @Test
    void SDK_9의_사용자_메타데이터에서_원본_파일명을_가져온다() throws Exception {
        String encodedFilename = "7Iqk66qo7YGsLmdpZg";
        StatObjectResponse stat = mock(StatObjectResponse.class);
        GetObjectResponse response = mock(GetObjectResponse.class);
        when(stat.size()).thenReturn(11L);
        when(stat.contentType()).thenReturn("image/gif");
        when(stat.userMetadata())
                .thenReturn(new Http.Headers(Map.of("original-filename", encodedFilename)));
        when(minioClient.statObject(any())).thenReturn(stat);
        when(minioClient.getObject(any())).thenReturn(response);

        StoredObject storedObject = objectStorage.get("file-id");

        assertThat(storedObject.metadata()).containsEntry("original-filename", encodedFilename);
        assertThat(storedObject.inputStream()).isSameAs(response);
    }
}
