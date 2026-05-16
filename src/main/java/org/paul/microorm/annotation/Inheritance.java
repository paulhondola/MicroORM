package org.paul.microorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks the root of an inheritance hierarchy and selects the mapping strategy. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Inheritance {
    InheritanceType strategy() default InheritanceType.SINGLE_TABLE;
}
