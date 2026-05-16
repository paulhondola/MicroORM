package org.paul.microorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {
    /** Name of the field on the owning (child) side that maps back to this entity. */
    String mappedBy() default "";
    CascadeType[] cascade() default {};
    boolean orphanRemoval() default false;
    FetchType fetch() default FetchType.EAGER;
}
