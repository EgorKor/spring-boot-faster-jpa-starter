package ru.samgtu.packages.webutils.annotations;

public @interface FilterMeta {
    Choices choices();
    String verboseName();
    String filter() default "";
}
