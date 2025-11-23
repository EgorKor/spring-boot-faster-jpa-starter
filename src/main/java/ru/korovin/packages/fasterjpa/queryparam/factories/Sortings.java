package ru.korovin.packages.fasterjpa.queryparam.factories;

import ru.korovin.packages.fasterjpa.queryparam.Sorting;
import ru.korovin.packages.fasterjpa.queryparam.sortingInternal.SortingUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Sortings {
    public static Sorting asc(String field) {
        return new Sorting(List.of(new SortingUnit(field, "asc")));
    }

    public static Sorting desc(String field) {
        return new Sorting(List.of(new SortingUnit(field, "desc")));
    }

    public static Sorting of(SortingUnit... sortingUnits) {
        return new Sorting(new ArrayList<>(Arrays.asList(sortingUnits)));
    }

    public static Sorting of(Collection<SortingUnit> sortingUnits) {
        return new Sorting(new ArrayList<>(sortingUnits));
    }
}
