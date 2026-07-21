package com.bop.youthpick.file.dto;

import java.io.InputStream;

public record FileDownload(
        String originalFilename, String contentType, long size, InputStream inputStream) {}
