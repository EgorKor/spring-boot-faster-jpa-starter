package ru.korovin.packages.fasterjpa.testProject.params;

import ru.korovin.packages.fasterjpa.testProject.model.TestEntity;
import ru.korovin.packages.fasterjpa.annotations.FieldParamMapping;
import ru.korovin.packages.fasterjpa.queryparam.Filter;

public class TestEntityFilter extends Filter<TestEntity> {
    private Long id;
    @FieldParamMapping(requestParamMapping = "name", sqlMapping = "_name")
    private String clientParamName;
}
