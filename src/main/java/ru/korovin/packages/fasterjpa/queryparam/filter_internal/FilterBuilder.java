package ru.korovin.packages.fasterjpa.queryparam.filter_internal;

import lombok.SneakyThrows;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.FilterAndCondition;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.FilterCondition;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.FilterConditionTreeNode;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.FilterOrCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterOperation.*;

public class FilterBuilder {

    public FilterAndCondition and(FilterConditionTreeNode... conditions) {
        return new FilterAndCondition(Arrays.asList(conditions));
    }

    public FilterAndCondition and(Collection<FilterConditionTreeNode> conditions) {
        return new FilterAndCondition(new ArrayList<>(conditions));
    }

    public FilterAndCondition or(FilterConditionTreeNode... conditions) {
        return new FilterAndCondition(Arrays.asList(conditions));
    }

    public FilterOrCondition or(Collection<FilterConditionTreeNode> conditions) {
        return new FilterOrCondition(new ArrayList<>(conditions));
    }

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

    public FilterCondition equalsIgnoreCase(String field, String value) {
        return new FilterCondition(field, EQUALS_IGNORE_CASE, value);
    }

}
