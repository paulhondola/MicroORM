package org.paul.microorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines the junction table for a @ManyToMany association. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinTable {
    String name() default "";
    /** FK column(s) referencing the owning entity. */
    JoinColumn[] joinColumns() default {};
    /** FK column(s) referencing the inverse entity. */
    JoinColumn[] inverseJoinColumns() default {};
}
