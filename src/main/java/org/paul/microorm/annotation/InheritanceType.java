package org.paul.microorm.annotation;

public enum InheritanceType {
    /** All classes in the hierarchy share one table with a discriminator column. */
    SINGLE_TABLE,
    /** Each class has its own table; subclass tables join to the parent. Not yet supported. */
    JOINED,
    /** Each concrete class has its own table with all inherited columns. Not yet supported. */
    TABLE_PER_CLASS
}
