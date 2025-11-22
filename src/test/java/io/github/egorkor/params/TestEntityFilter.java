package io.github.egorkor.params;

import io.github.egorkor.model.TestEntity;
import ru.samgtu.packages.webutils.annotations.FieldParamMapping;
import ru.samgtu.packages.webutils.queryparam.Filter;

public class TestEntityFilter extends Filter<TestEntity> {
    private Long id;
    @FieldParamMapping(requestParamMapping = "name", sqlMapping = "_name")
    private String clientParamName;
}
