package com.bop.youthpick.file.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.file.dto.FileDownload;
import com.bop.youthpick.file.dto.FileUploadResponse;
import com.bop.youthpick.file.service.FileService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "파일")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private static final Duration FILE_CACHE_MAX_AGE = Duration.ofDays(365);

    private final FileService fileService;

    @Operation(summary = "파일 업로드", description = "인증된 사용자가 파일을 업로드하고 저장된 파일 정보를 반환한다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @CurrentUser Long userId, @RequestPart("file") MultipartFile file) {
        FileUploadResponse response = fileService.upload(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "파일 다운로드", description = "파일 ID로 저장된 파일을 조회해 스트림으로 반환한다.")
    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
        FileDownload file = fileService.download(fileId);
        ContentDisposition contentDisposition =
                ContentDisposition.inline()
                        .filename(file.originalFilename(), StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(FILE_CACHE_MAX_AGE).cachePublic().immutable())
                .body(new InputStreamResource(file.inputStream()));
    }
}
