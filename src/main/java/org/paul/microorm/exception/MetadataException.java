package org.paul.microorm.exception;

public class MetadataException extends MicroOrmException {
    public MetadataException(String message) {
        super(message);
    }

    public MetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
