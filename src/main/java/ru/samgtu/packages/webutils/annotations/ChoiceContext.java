package ru.samgtu.packages.webutils.annotations;

import org.aspectj.apache.bcel.generic.TABLESWITCH;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChoiceContext {
    String key() default "";
    String value() default "";
}
