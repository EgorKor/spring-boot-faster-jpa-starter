package ru.samgtu.packages.webutils.annotations;

import ru.samgtu.packages.webutils.queryparam.filterInternal.FilterOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedOperations {
    FilterOperation[] value() default {FilterOperation.EQUALS};
}
