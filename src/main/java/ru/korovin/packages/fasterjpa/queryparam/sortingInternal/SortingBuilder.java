package ru.korovin.packages.fasterjpa.queryparam.sortingInternal;

import ru.korovin.packages.fasterjpa.queryparam.Sorting;
import lombok.SneakyThrows;

import java.util.Arrays;

import static ru.korovin.packages.fasterjpa.queryparam.Sorting.ASC;
import static ru.korovin.packages.fasterjpa.queryparam.Sorting.DESC;

public class SortingBuilder {
    public SortingUnit asc(String field) {
        return new SortingUnit(field, ASC);
    }

    public SortingUnit desc(String field) {
        return new SortingUnit(field, DESC);
    }

    public Sorting by(SortingUnit... sortingUnits) {
        return new Sorting(Arrays.asList(sortingUnits));
    }

    @SneakyThrows
    public <R extends Sorting> R by(Class<R> derivedClass, SortingUnit... sortingUnits) {
        R derivedSort = derivedClass.getDeclaredConstructor().newInstance();
        derivedSort.setSort(Arrays.asList(sortingUnits));
        return derivedSort;
    }
}
