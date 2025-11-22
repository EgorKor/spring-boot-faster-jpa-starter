package io.github.egorkor.params;

import ru.samgtu.packages.webutils.annotations.ParamCountLimit;
import ru.samgtu.packages.webutils.queryparam.Sorting;

@ParamCountLimit(1)
public class UserSort extends Sorting {
    private Long id;
    private String name;
}
