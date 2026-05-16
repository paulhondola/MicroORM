package org.paul.microorm.exception;

public class MicroOrmException extends RuntimeException {
    public MicroOrmException(String message) {
        super(message);
    }

    public MicroOrmException(String message, Throwable cause) {
        super(message, cause);
    }
}
