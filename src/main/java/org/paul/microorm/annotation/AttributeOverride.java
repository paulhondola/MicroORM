package org.paul.microorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Overrides a column mapping for an @Embedded field's attribute. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Repeatable(AttributeOverrides.class)
public @interface AttributeOverride {
    /** Name of the field in the @Embeddable class. */
    String name();
    /** The column definition to use instead of the default. */
    Column column();
}
