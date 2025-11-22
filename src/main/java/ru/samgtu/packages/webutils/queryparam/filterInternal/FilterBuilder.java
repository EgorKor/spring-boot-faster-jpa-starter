package ru.samgtu.packages.webutils.queryparam.filterInternal;

import ru.samgtu.packages.webutils.queryparam.Filter;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static ru.samgtu.packages.webutils.queryparam.filterInternal.FilterOperation.*;

public class FilterBuilder {

    public FilterCondition equals(String field, Object value) {
        return new FilterCondition(field, EQUALS, value);
    }

    public FilterCondition notEquals(String field, Object value) {
        return new FilterCondition(field, NOT_EQUALS, value);
    }

    public FilterCondition less(String field, Comparable<?> value) {
        return new FilterCondition(field, LS, value);
    }

    public FilterCondition lessOrEquals(String field, Comparable<?> value) {
        return new FilterCondition(field, LSE, value);
    }

    public FilterCondition greater(String field, Comparable<?> value) {
        return new FilterCondition(field, GT, value);
    }

    public FilterCondition greaterOrEquals(String field, Comparable<?> value) {
        return new FilterCondition(field, GTE, value);
    }

    public FilterCondition like(String field, String value) {
        return new FilterCondition(field, LIKE, value);
    }

    public FilterCondition contains(String field, String value) {
        return new FilterCondition(field, CONTAINS, value);
    }

    public FilterCondition notContains(String field, String value) {
        return new FilterCondition(field, NOT_CONTAINS, value);
    }

    public FilterCondition in(String field, Object... values) {
        return new FilterCondition(field, IN, Arrays.asList(values));
    }

    public FilterCondition inCollection(String field, Collection<?> values) {
        return new FilterCondition(field, IN, values);
    }

    public FilterCondition is(String field, Is value) {
        return new FilterCondition(field, IS, value.getValue());
    }

    public FilterCondition notLike(String field, String value) {
        return new FilterCondition(field, NOT_LIKE, value);
    }

    public FilterCondition notIn(String field, Object... values) {
        return new FilterCondition(field, NOT_IN, Arrays.asList(values));
    }

    public FilterCondition notInCollection(String field, Collection<?> values) {
        return new FilterCondition(field, NOT_IN, values);
    }

    public FilterCondition equalsIgnoreCase(String field, String value){
        return new FilterCondition(field, EQUALS_IGNORE_CASE, value);
    }

    public <T> Filter<T> and(FilterCondition... operations) {
        return new Filter<>(Arrays.asList(operations));
    }

    public <T> Filter<T> and(Collection<FilterCondition> operations){
        return new Filter<>(new ArrayList<>(operations));
    }

    @SneakyThrows
    public <T extends Filter<?>> T and(Class<T> derivedType, FilterCondition... operations) {
        T derivedFilter = derivedType.getDeclaredConstructor().newInstance();
        derivedFilter.setConditions(Arrays.asList(operations));
        return derivedFilter;
    }

    @SneakyThrows
    public <T extends Filter<?>> T and(Class<T> derivedType, Collection<FilterCondition> operations) {
        T derivedFilter = derivedType.getDeclaredConstructor().newInstance();
        derivedFilter.setConditions(new ArrayList<>(operations));
        return derivedFilter;
    }

}
