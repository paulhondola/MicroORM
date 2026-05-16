package org.paul.microorm.annotation;

public enum FetchType {
    EAGER,
    /** LAZY is accepted but honoured as EAGER in this implementation. */
    LAZY
}
