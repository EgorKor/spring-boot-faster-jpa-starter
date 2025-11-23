package ru.korovin.packages.fasterjpa.testProject.params;

import ru.korovin.packages.fasterjpa.annotations.ParamCountLimit;
import ru.korovin.packages.fasterjpa.queryparam.Sorting;

@ParamCountLimit(1)
public class UserSort extends Sorting {
    private Long id;
    private String name;
}
