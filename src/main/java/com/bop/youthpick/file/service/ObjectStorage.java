package com.bop.youthpick.file.service;

import java.io.InputStream;
import java.util.Map;

public interface ObjectStorage {

    void put(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType,
            Map<String, String> metadata)
            throws ObjectStorageException;

    StoredObject get(String objectKey) throws ObjectStorageException;
}
