package ru.korovin.packages.fasterjpa.annotations;

public @interface FilterMeta {
    Choices choices();
    String verboseName();
    String filter() default "";
}
