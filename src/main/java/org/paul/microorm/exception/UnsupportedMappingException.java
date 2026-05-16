package org.paul.microorm.exception;

import org.paul.microorm.annotation.InheritanceType;

public class UnsupportedMappingException extends MicroOrmException {
    public UnsupportedMappingException(InheritanceType strategy) {
        super("Inheritance strategy not supported: " + strategy
                + ". Only SINGLE_TABLE is implemented. Use @Inheritance(strategy = InheritanceType.SINGLE_TABLE).");
    }

    public UnsupportedMappingException(String message) {
        super(message);
    }
}
