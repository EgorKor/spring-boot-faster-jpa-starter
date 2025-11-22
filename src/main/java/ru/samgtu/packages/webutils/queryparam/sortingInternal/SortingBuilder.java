package ru.samgtu.packages.webutils.queryparam.sortingInternal;

import ru.samgtu.packages.webutils.queryparam.Sorting;
import lombok.SneakyThrows;

import java.util.Arrays;

import static ru.samgtu.packages.webutils.queryparam.Sorting.ASC;
import static ru.samgtu.packages.webutils.queryparam.Sorting.DESC;

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
