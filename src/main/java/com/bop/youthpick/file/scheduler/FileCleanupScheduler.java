package com.bop.youthpick.file.scheduler;

import com.bop.youthpick.file.service.MinioProperties;
import com.bop.youthpick.post.repository.AttachmentRepository;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final AttachmentRepository attachmentRepository;

    // 매주 일요일 새벽 3시에 고아 파일 정리 실행 (0 0 3 * * SUN)
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupOrphanFiles() {
        log.info("고아 파일(미참조 이미지) 정리 스케줄러 시작");
        try {
            // 1. DB의 모든 첨부파일 URL을 가져온 후 파일명(또는 키) 추출
            Set<String> dbFileKeys =
                    attachmentRepository.findAll().stream()
                            .map(attachment -> extractFileKey(attachment.getFileUrl()))
                            .filter(key -> key != null)
                            .collect(Collectors.toSet());

            // 2. MinIO 버킷의 모든 객체 목록 긁어오기
            Iterable<Result<Item>> results =
                    minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(properties.bucket()).build());

            List<String> orphanKeys = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectKey = item.objectName();
                // DB에 해당 objectKey가 존재하지 않으면 고아 파일로 판단
                if (!dbFileKeys.contains(objectKey)) {
                    orphanKeys.add(objectKey);
                }
            }

            log.info("발견된 고아 파일 개수: {}개", orphanKeys.size());

            // 3. 고아 파일 삭제 진행
            for (String key : orphanKeys) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(properties.bucket())
                                    .object(key)
                                    .build());
                    log.info("고아 파일 삭제 완료: {}", key);
                } catch (Exception e) {
                    log.error("고아 파일 삭제 실패: {}", key, e);
                }
            }

            log.info("고아 파일 정리 스케줄러 종료");
        } catch (Exception e) {
            log.error("고아 파일 정리 스케줄러 작업 중 오류 발생", e);
        }
    }

    private String extractFileKey(String fileUrl) {
        if (fileUrl == null) {
            return null;
        }
        int lastSlashIndex = fileUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            return fileUrl.substring(lastSlashIndex + 1);
        }
        return fileUrl;
    }
}
