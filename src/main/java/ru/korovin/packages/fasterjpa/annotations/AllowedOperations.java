package ru.korovin.packages.fasterjpa.annotations;

import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedOperations {
    FilterOperation[] value() default {FilterOperation.EQUALS};
}
