package com.bop.youthpick.file.service;

public class ObjectStorageException extends Exception {

    private final boolean objectNotFound;

    private ObjectStorageException(boolean objectNotFound, Throwable cause) {
        super(cause);
        this.objectNotFound = objectNotFound;
    }

    public static ObjectStorageException objectNotFound(Throwable cause) {
        return new ObjectStorageException(true, cause);
    }

    public static ObjectStorageException unavailable(Throwable cause) {
        return new ObjectStorageException(false, cause);
    }

    public boolean isObjectNotFound() {
        return objectNotFound;
    }
}
