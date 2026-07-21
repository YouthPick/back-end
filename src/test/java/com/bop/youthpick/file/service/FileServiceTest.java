package com.bop.youthpick.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.file.dto.FileDownload;
import com.bop.youthpick.file.dto.FileUploadResponse;
import com.bop.youthpick.file.exception.FileErrorCode;
import com.bop.youthpick.file.exception.FileException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final byte[] PNG =
            new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Mock private ObjectStorage objectStorage;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(objectStorage, new FileProperties(DataSize.ofMegabytes(10)));
    }

    @Test
    void PNG_이미지를_업로드하고_백엔드_다운로드_URL을_반환한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "프로필.png", "image/png", PNG);

        FileUploadResponse response = fileService.upload(7L, file);

        assertThat(response.originalFilename()).isEqualTo("프로필.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.downloadUrl()).isEqualTo("/api/v1/files/" + response.fileId());
        verify(objectStorage)
                .put(anyString(), any(InputStream.class), anyLong(), anyString(), anyMap());
    }

    @Test
    void 경로가_포함된_파일명은_마지막_이름만_저장한다() {
        MockMultipartFile file =
                new MockMultipartFile("file", "../unsafe/photo.png", "image/png", PNG);

        FileUploadResponse response = fileService.upload(7L, file);

        assertThat(response.originalFilename()).isEqualTo("photo.png");
    }

    @Test
    void MIME만_이미지로_위장한_파일은_거부한다() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "fake.png",
                        "image/png",
                        "not-an-image".getBytes(StandardCharsets.UTF_8));

        assertFileError(file, FileErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void 설정된_최대_크기를_초과하면_거부한다() {
        fileService = new FileService(objectStorage, new FileProperties(DataSize.ofBytes(8)));
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", PNG);

        assertFileError(file, FileErrorCode.FILE_SIZE_EXCEEDED);
    }

    @Test
    void 설정된_최대_크기와_같은_파일은_허용한다() {
        fileService =
                new FileService(objectStorage, new FileProperties(DataSize.ofBytes(PNG.length)));
        MockMultipartFile file = new MockMultipartFile("file", "boundary.png", "image/png", PNG);

        FileUploadResponse response = fileService.upload(7L, file);

        assertThat(response.size()).isEqualTo(PNG.length);
    }

    @Test
    void 저장소_업로드_장애를_S002로_변환한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG);
        doThrow(ObjectStorageException.unavailable(new IllegalStateException()))
                .when(objectStorage)
                .put(anyString(), any(), anyLong(), anyString(), anyMap());

        assertThatThrownBy(() -> fileService.upload(7L, file))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.STORAGE_UNAVAILABLE);
    }

    @Test
    void 파일을_다운로드하면_원본명과_스트림을_반환한다() throws Exception {
        UUID fileId = UUID.randomUUID();
        String encodedFilename =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString("사진.png".getBytes(StandardCharsets.UTF_8));
        when(objectStorage.get(fileId.toString()))
                .thenReturn(
                        new StoredObject(
                                new ByteArrayInputStream(PNG),
                                PNG.length,
                                "image/png",
                                Map.of("original-filename", encodedFilename)));

        FileDownload response = fileService.download(fileId);

        assertThat(response.originalFilename()).isEqualTo("사진.png");
        assertThat(response.inputStream().readAllBytes()).isEqualTo(PNG);
    }

    @Test
    void 존재하지_않는_파일은_C003으로_변환한다() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(objectStorage.get(fileId.toString()))
                .thenThrow(ObjectStorageException.objectNotFound(new IllegalStateException()));

        assertThatThrownBy(() -> fileService.download(fileId))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_FOUND);
    }

    private void assertFileError(MockMultipartFile file, FileErrorCode errorCode) {
        assertThatThrownBy(() -> fileService.upload(7L, file))
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
