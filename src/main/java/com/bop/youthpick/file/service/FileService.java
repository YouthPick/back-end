package com.bop.youthpick.file.service;

import com.bop.youthpick.file.dto.FileDownload;
import com.bop.youthpick.file.dto.FileUploadResponse;
import com.bop.youthpick.file.exception.FileErrorCode;
import com.bop.youthpick.file.exception.FileException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final String ORIGINAL_FILENAME_METADATA = "original-filename";
    private static final int SIGNATURE_LENGTH = 12;
    private static final int MAX_FILENAME_LENGTH = 255;

    private final ObjectStorage objectStorage;
    private final long maxFileSize;

    public FileService(ObjectStorage objectStorage, FileProperties properties) {
        this.objectStorage = objectStorage;
        this.maxFileSize = properties.maxSize().toBytes();
    }

    public FileUploadResponse upload(Long userId, MultipartFile file) {
        validate(file);

        UUID fileId = UUID.randomUUID();
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        Map<String, String> metadata =
                Map.of(
                        ORIGINAL_FILENAME_METADATA,
                        encodeFilename(originalFilename),
                        "uploaded-by",
                        userId.toString());

        try (InputStream inputStream = file.getInputStream()) {
            objectStorage.put(
                    fileId.toString(),
                    inputStream,
                    file.getSize(),
                    file.getContentType(),
                    metadata);
        } catch (IOException | ObjectStorageException exception) {
            log.warn("파일 저장소 업로드에 실패했습니다. fileId={}, userId={}", fileId, userId, exception);
            throw new FileException(FileErrorCode.STORAGE_UNAVAILABLE);
        }

        return new FileUploadResponse(
                fileId,
                originalFilename,
                file.getContentType(),
                file.getSize(),
                "/api/v1/files/" + fileId);
    }

    public FileDownload download(UUID fileId) {
        try {
            StoredObject storedObject = objectStorage.get(fileId.toString());
            String originalFilename =
                    decodeFilename(storedObject.metadata().get(ORIGINAL_FILENAME_METADATA));
            return new FileDownload(
                    originalFilename,
                    storedObject.contentType(),
                    storedObject.size(),
                    storedObject.inputStream());
        } catch (ObjectStorageException exception) {
            if (exception.isObjectNotFound()) {
                throw new FileException(FileErrorCode.FILE_NOT_FOUND);
            }
            log.warn("파일 저장소 조회에 실패했습니다. fileId={}", fileId, exception);
            throw new FileException(FileErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(FileErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > maxFileSize) {
            throw new FileException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType()) || !hasValidSignature(file)) {
            throw new FileException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private boolean hasValidSignature(MultipartFile file) {
        byte[] signature = new byte[SIGNATURE_LENGTH];
        try (InputStream inputStream = file.getInputStream()) {
            int length = inputStream.read(signature);
            return switch (file.getContentType()) {
                case "image/jpeg" -> isJpeg(signature, length);
                case "image/png" -> isPng(signature, length);
                case "image/gif" -> isGif(signature, length);
                case "image/webp" -> isWebp(signature, length);
                default -> false;
            };
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean isJpeg(byte[] signature, int length) {
        return length >= 3
                && unsigned(signature[0]) == 0xFF
                && unsigned(signature[1]) == 0xD8
                && unsigned(signature[2]) == 0xFF;
    }

    private boolean isPng(byte[] signature, int length) {
        return length >= 8
                && unsigned(signature[0]) == 0x89
                && signature[1] == 'P'
                && signature[2] == 'N'
                && signature[3] == 'G'
                && unsigned(signature[4]) == 0x0D
                && unsigned(signature[5]) == 0x0A
                && unsigned(signature[6]) == 0x1A
                && unsigned(signature[7]) == 0x0A;
    }

    private boolean isGif(byte[] signature, int length) {
        return length >= 6
                && (asciiEquals(signature, 0, "GIF87a") || asciiEquals(signature, 0, "GIF89a"));
    }

    private boolean isWebp(byte[] signature, int length) {
        return length >= SIGNATURE_LENGTH
                && asciiEquals(signature, 0, "RIFF")
                && asciiEquals(signature, 8, "WEBP");
    }

    private boolean asciiEquals(byte[] bytes, int offset, String expected) {
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }
        String normalized = filename.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1);
        String sanitized = basename.replaceAll("[\\r\\n\\u0000]", "").trim();
        if (sanitized.isBlank()) {
            return "image";
        }
        return sanitized.length() > MAX_FILENAME_LENGTH
                ? sanitized.substring(0, MAX_FILENAME_LENGTH)
                : sanitized;
    }

    private String encodeFilename(String filename) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(filename.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeFilename(String encodedFilename) {
        if (encodedFilename == null || encodedFilename.isBlank()) {
            return "image";
        }
        try {
            return new String(
                    Base64.getUrlDecoder().decode(encodedFilename), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "image";
        }
    }
}
