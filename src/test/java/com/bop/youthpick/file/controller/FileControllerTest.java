package com.bop.youthpick.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.file.dto.FileDownload;
import com.bop.youthpick.file.dto.FileUploadResponse;
import com.bop.youthpick.file.service.FileService;
import com.bop.youthpick.global.error.GlobalExceptionHandler;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private FileService fileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new FileController(fileService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
                        .build();
    }

    @Test
    void 파일을_업로드하면_201과_백엔드_URL을_반환한다() throws Exception {
        UUID fileId = UUID.randomUUID();
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1});
        when(fileService.upload(eq(1L), any()))
                .thenReturn(
                        new FileUploadResponse(
                                fileId, "photo.png", "image/png", 1, "/api/v1/files/" + fileId));

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileId").value(fileId.toString()))
                .andExpect(jsonPath("$.data.downloadUrl").value("/api/v1/files/" + fileId));
    }

    @Test
    void multipart에_파일이_없으면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/v1/files"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("file"));
    }

    @Test
    void 파일을_inline으로_스트리밍한다() throws Exception {
        UUID fileId = UUID.randomUUID();
        byte[] bytes = new byte[] {1, 2, 3};
        when(fileService.download(fileId))
                .thenReturn(
                        new FileDownload(
                                "사진.png",
                                "image/png",
                                bytes.length,
                                new ByteArrayInputStream(bytes)));

        mockMvc.perform(get("/api/v1/files/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType("image/png"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "inline; filename=\"=?UTF-8?Q?=EC=82=AC=EC=A7=84.png?=\"; filename*=UTF-8''%EC%82%AC%EC%A7%84.png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CACHE_CONTROL,
                                        "max-age=31536000, public, immutable"));
    }

    private static class TestCurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return 1L;
        }
    }
}
