package com.bop.youthpick.file.dto;

import java.util.UUID;

public record FileUploadResponse(
        UUID fileId, String originalFilename, String contentType, long size, String downloadUrl) {}
