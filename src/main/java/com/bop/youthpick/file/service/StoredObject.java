package com.bop.youthpick.file.service;

import java.io.InputStream;
import java.util.Map;

public record StoredObject(
        InputStream inputStream, long size, String contentType, Map<String, String> metadata) {}
